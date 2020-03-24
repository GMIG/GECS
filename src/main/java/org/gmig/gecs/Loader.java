package org.gmig.gecs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TabPane;
import org.gmig.gecs.device.Device;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.device.Switchable;
import org.gmig.gecs.factories.*;
import org.gmig.gecs.groups.*;
import org.gmig.gecs.views.DeviceView;
import org.gmig.gecs.views.SwitchGroupView;
import org.quartz.SchedulerException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
public class Loader {

    private String structureFile = "structure.json";
    private String specialScheduleFile = "specialSchedule.json";
    private String cronScheduleFile = "cronSchedule.json";
    private String viewTypesFile = "viewTypes.json";
    private String viewsFile = "views.json";
    private String sourcesFile = "sources.json";
    private String botsFile = "bots.json";


    private final SourcesServer server = new SourcesServer();
    private SwitchGroupScheduler scheduler;
    private HashSet<SwitchGroupView> switchGroupViews = new HashSet<>();

    public static File getFile(String name) throws UnsupportedEncodingException {
        URL url = Loader.class.getProtectionDomain().getCodeSource().getLocation();
        String jarPath = URLDecoder.decode(url.getFile(), "UTF-8");
        if(jarPath.contains("jar")) {
            jarPath = jarPath.substring(0,jarPath.lastIndexOf("/") + 1);
        }
        return new File(jarPath + name);
    }

    private String loadFileToString(String name) throws IOException {
        File file = getFile(name);
        URI uri = file.toURI();
        Path path = Paths.get(uri);
        StringBuilder data = new StringBuilder();
        Stream<String> lines = Files.lines(path);
        lines.forEach(line -> data.append(line).append("\n"));
        lines.close();
        return data.toString().trim();
    }

    void load(TabPane whereTo) {
        StructureReader.factories.add(new DatatonFactory());
        StructureReader.factories.add(new ProjectorFactory());
        StructureReader.factories.add(new SharpTVFactory());
        StructureReader.factories.add(new VLCPlayer2ScreenFactory());
        StructureReader.factories.add(new VLCPlayerFactory());
        StructureReader.factories.add(new VLCTestPlayerFactory());
        StructureReader.factories.add(new DocCentrFactory());
        StructureReader.factories.add(new PCWithDaemonFactory());

        try {
            String structure = loadFileToString(structureFile);
            HashSet<ManagedDevice> devices = StructureReader.loadDevicesFromJSON(structure);
            HashSet<Module> modules = StructureReader.loadModulesFromJSON(devices, structure);
            HashSet<VisModule> vismodules = StructureReader.loadVisModulesFromJSON(devices, structure);
            HashSet<Watchdog> watchdogs = new HashSet<>();
            vismodules.forEach((mod)->{
                if(mod.getSource().checkedRestartCmd()!=null) {
                    Watchdog dog = new Watchdog(mod);
                    watchdogs.add(dog);
                }
            });

            modules.addAll(vismodules);

            HashSet<Switchable> all = new HashSet<>();
            all.addAll(devices);
            all.addAll(modules);

            HashSet<SwitchGroup> switchGroups = StructureReader.loadSwitchersFromJSON(all, structure);

            HashSet<Switchable> allCommandables = new HashSet<>();
            allCommandables.addAll(devices);
            allCommandables.addAll(modules);
            allCommandables.addAll(switchGroups);

            String botsString = loadFileToString(botsFile);
            Set<TBot> bots = StructureReader.loadBotsFromJSON(allCommandables,botsString);

            watchdogs.forEach((wd)->wd.onKilledByCheck.add(
                    (t)-> {
                        bots.forEach(
                                (b)-> b.sendMessageToAllChats("Судя по всему сломался "
                                        + wd.module.getSource().getDescription()
                                        + ". Я попытался его починить, но ничего не вышло. Я его выключил. Придется подождать"));
                    })
            );

            watchdogs.forEach((wd)->wd.onKilledBySwitchOn.add(
                    (t)-> {
                        bots.forEach(
                                (b)-> b.sendMessageToAllChats("Судя по всему сломался "
                                        + wd.module.getSource().getDescription()
                                        + ". Я попытался его починить, но ничего не вышло. Я его выключил. Придется подождать"));
                    })
            );

            String sourcesString = loadFileToString(sourcesFile);
            StructureReader.loadSourcesToServerFromJSON(allCommandables, server, sourcesString);

            scheduler = new SwitchGroupScheduler(switchGroups);
            //scheduler.updateSpecialSchedule(Date.from(Instant.now().plus(1,ChronoUnit.DAYS)),"cafe",Date.from(Instant.now()),Date.from(Instant.now()),getFile(specialScheduleFile));

            if (switchGroups.isEmpty())
                throw new ClassNotFoundException("No groups found in structure.json");

            String specialSchedule = loadFileToString(specialScheduleFile);
            String cronSchedule = loadFileToString(cronScheduleFile);
            scheduler.loadSchedule(cronSchedule, specialSchedule);

            String viewTypes = loadFileToString(viewTypesFile);
            String views = loadFileToString(viewsFile);

            ObjectMapper mapper = new ObjectMapper();

            for (SwitchGroup switchGroup : switchGroups) {
                JsonNode viewsNode = mapper.readTree(views).get("switchGroups");

                SwitchGroupView switchGroupView = new SwitchGroupView(switchGroup, viewsNode, whereTo, watchdogs);
                switchGroupViews.add(switchGroupView);
                switchGroupView.schedulerView.updateDates(scheduler, switchGroup);
                Set<ManagedDevice> devicesOfGroup = switchGroup.getAllChildren().stream()
                        .filter((sw) -> sw instanceof ManagedDevice)
                        .map((sw) -> (ManagedDevice) sw)
                        .collect(Collectors.toSet());

                @SuppressWarnings("SuspiciousMethodCalls")
                Set<Device> sourcesOfGroup = server.getCommandables().entrySet().stream()
                        .filter((entry) -> switchGroup.getAllChildren().contains(entry.getKey()) || switchGroup.equals(entry.getKey()))
                        .map(Map.Entry::getValue)
                        .collect(Collectors.toSet());

                Set<Device> sourcesAndDevices = new HashSet<>();
                sourcesAndDevices.addAll(devicesOfGroup);
                sourcesAndDevices.addAll(sourcesOfGroup);

                Set<DeviceView> viewsSet = StructureReader.loadDeviceViewsFromJSON(sourcesAndDevices, viewTypes, views);
                viewsSet.forEach((dv) ->
                        dv.addToPane(switchGroupView.placePlanPane));
                switchGroupView.set.setOnMouseClicked((e) -> {
                    LocalDate dt = ((DatePicker) switchGroupView.tab.getContent().lookup("#date")).getValue();
                    try {
                        String times[]=switchGroupView.checkFields();
                        scheduler.updateSpecialSchedule(
                                Date.from(dt.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                                switchGroup.getName(),
                                times[0],
                                times[1],
                                getFile(specialScheduleFile));
                        scheduler.loadSchedule(cronSchedule, loadFileToString(specialScheduleFile));
                        switchGroupView.schedulerView.updateDates(scheduler, switchGroup);
                    } catch (ParseException | SchedulerException | IOException e1) {
                        e1.printStackTrace();
                    }
                });
                switchGroupView.init.exec();
            }
        }
        catch (ClassNotFoundException | ParseException | SchedulerException | IOException e){
            e.printStackTrace();
        }
    }

    void dispose(){
        StructureReader.factories.forEach(AbstractManagedDeviceFactory::dispose);
        server.dispose();
        switchGroupViews.forEach(SwitchGroupView::disposeTimer);
        try {
            scheduler.dispose();
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }


}
