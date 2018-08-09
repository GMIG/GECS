package org.gmig.gecs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.geometry.Insets;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import org.gmig.gecs.device.Device;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.device.Switchable;
import org.gmig.gecs.factories.*;
import org.gmig.gecs.groups.Module;
import org.gmig.gecs.groups.SwitchGroup;
import org.gmig.gecs.groups.SwitchGroupScheduler;
import org.gmig.gecs.views.DeviceView;
import org.gmig.gecs.views.SwitchGroupView;
import org.quartz.SchedulerException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by brix on 4/26/2018.
 */
public class Loader {

    String structureFile = "structure.json";
    String specialScheduleFile = "specialSchedule.json";
    String cronScheduleFile = "cronSchedule.json";
    String viewTypesFile = "viewTypes.json";
    String viewsFile = "views.json";
    String sourcesFile = "sources.json";


    public static File getFile(String name){
        URL url = Loader.class.getProtectionDomain().getCodeSource().getLocation();
        String jarPath = null;
        try {
            jarPath = URLDecoder.decode(url.getFile(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if(jarPath.contains("jar")) {
            jarPath = jarPath.substring(0,jarPath.lastIndexOf("/") + 1);

        }
        return new File(jarPath + name);
    }

    String loadFileToString(String name) throws IOException {
        Path path = Paths.get(getFile(name).toURI());
        StringBuilder data = new StringBuilder();
        Stream<String> lines = Files.lines(path);
        lines.forEach(line -> data.append(line).append("\n"));
        lines.close();
        return data.toString().trim();
    }

    public void load(TabPane whereTo) throws IOException, SchedulerException, ParseException, ClassNotFoundException, InterruptedException {
        //StructureReader reader = new StructureReader();
        StructureReader.factories.add(new DatatonFactory());
        StructureReader.factories.add(new ProjectorFactory());
        StructureReader.factories.add(new SharpTVFactory());
        StructureReader.factories.add(new VLCPlayer2ScreenFactory());
        StructureReader.factories.add(new VLCPlayerFactory());
        StructureReader.factories.add(new InfokioskFactory());
        StructureReader.factories.add(new VLCTestPlayerFactory());

        String structure = loadFileToString(structureFile);
        HashSet<ManagedDevice> devices = StructureReader.loadDevicesFromJSON(structure);

        HashSet<Module> modules = StructureReader.loadModulesFromJSON(devices, structure);

        HashSet<Switchable> all = new HashSet<>();
        all.addAll(devices);
        all.addAll(modules);

        HashSet<SwitchGroup> switchGroups = StructureReader.loadSwitchersFromJSON(all, structure);

        String sourcesString = loadFileToString(sourcesFile);
        SourcesServer server = new SourcesServer();
        HashSet<Device> sources = StructureReader.loadSourcesFromJSON(devices,server,sourcesString);

        SwitchGroupScheduler scheduler = new SwitchGroupScheduler(switchGroups);
        //scheduler.updateSpecialSchedule(Date.from(Instant.now().plus(1,ChronoUnit.DAYS)),"cafe",Date.from(Instant.now()),Date.from(Instant.now()),getFile(specialScheduleFile));

        if (switchGroups == null)
            return;

        String specialSchedule = loadFileToString(specialScheduleFile);
        String cronSchedule = loadFileToString(cronScheduleFile);
        scheduler.loadSchedule(cronSchedule, specialSchedule);

        String viewTypes = loadFileToString(viewTypesFile);
        String views = loadFileToString(viewsFile);

        ObjectMapper mapper = new ObjectMapper();

        for (SwitchGroup switchGroup : switchGroups) {
            JsonNode viewsNode = mapper.readTree(views).get("switchGroups");

            SwitchGroupView switchGroupView = new SwitchGroupView(switchGroup,viewsNode, whereTo);
            switchGroupView.schedulerView.updateDates(scheduler,switchGroup);
            Set<ManagedDevice> devicesOfGroup = switchGroup.getAllChildren().stream()
                    .filter((sw) -> sw instanceof ManagedDevice)
                    .map((sw) -> (ManagedDevice) sw).collect(Collectors.toSet());
            Set<Device> sourcesOfGroup = server.getDevices().entrySet().stream()
                    .filter((entry) -> devicesOfGroup.contains(entry.getKey()))
                    .map((entry)->entry.getValue()).collect(Collectors.toSet());

            Set<Device> sourcesAndDevices = new HashSet<>();
            sourcesAndDevices.addAll(devicesOfGroup);
            sourcesAndDevices.addAll(sourcesOfGroup);

            Set<DeviceView> viewsSet = StructureReader.loadDeviceViewsFromJSON(sourcesAndDevices, viewTypes, views);
            viewsSet.forEach((dv) ->
                    dv.addToPane(switchGroupView.placePlanPane));
            switchGroupView.set.setOnMouseClicked((e)->{
                LocalDate dt = ((DatePicker)switchGroupView.tab.getContent().lookup("#date")).getValue();
                TextField txtTurnOnHours = (TextField)switchGroupView.tab.getContent().lookup("#txtTurnOnHours");
                TextField txtTurnOnMinutes = (TextField)switchGroupView.tab.getContent().lookup("#txtTurnOnMinutes");
                TextField txtTurnOffHours = (TextField)switchGroupView.tab.getContent().lookup("#txtTurnOffHours");
                TextField txtTurnOffMinutes = (TextField)switchGroupView.tab.getContent().lookup("#txtTurnOffMinutes");
                // Sono stanchissimo. Non posso pensare piu. Caro io di futuro, non essere arrabiato.
                try {
                    String timeOn = "0 " + txtTurnOnMinutes.getText() + " " + txtTurnOnHours.getText();
                    String timeOff = "0 " + txtTurnOffMinutes.getText() +" " + txtTurnOffHours.getText();
                    try {
                        SwitchGroupScheduler.timedateFormatDecoder.parse("1 1 2017 " + timeOn);
                        ((Label) switchGroupView.tab.getContent().lookup("#txtTurnOnTimeLabel")).setBackground(
                                new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));

                    }
                    catch (ParseException ex){
                        ((Label) switchGroupView.tab.getContent().lookup("#txtTurnOnTimeLabel")).setBackground(
                                new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
                        timeOn="";
                    }
                    try {
                        SwitchGroupScheduler.timedateFormatDecoder.parse("1 1 2017 " + timeOff);
                        ((Label) switchGroupView.tab.getContent().lookup("#txtTurnOffTimeLabel")).setBackground(
                                new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));

                    }
                    catch (ParseException ex){
                        ((Label) switchGroupView.tab.getContent().lookup("#txtTurnOffTimeLabel")).setBackground(
                                new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
                        timeOff="";
                    }

                    scheduler.updateSpecialSchedule(
                            Date.from(dt.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                            switchGroup.getName(),
                            timeOn ,
                            timeOff,
                            getFile(specialScheduleFile));
                    scheduler.loadSchedule(cronSchedule, loadFileToString(specialScheduleFile));
                    switchGroupView.schedulerView.updateDates(scheduler,switchGroup);
                } catch (ParseException | SchedulerException | IOException e1) {
                    e1.printStackTrace();
                }
            });

            switchGroupView.init.exec();

        }

    }


}
