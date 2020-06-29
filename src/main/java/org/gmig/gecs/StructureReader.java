package org.gmig.gecs;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.gmig.gecs.device.Commandable;
import org.gmig.gecs.device.Device;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.device.Switchable;
import org.gmig.gecs.factories.AbstractManagedDeviceFactory;
import org.gmig.gecs.groups.Module;
import org.gmig.gecs.groups.SwitchGroup;
import org.gmig.gecs.groups.VisModule;
import org.gmig.gecs.views.DeviceView;
import org.quartz.SimpleTrigger;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *
 */
public final class StructureReader {

    public static final HashSet<AbstractManagedDeviceFactory> factories = new HashSet<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    static HashSet<ManagedDevice> loadDevicesFromJSON(String json) throws IOException, IllegalArgumentException, ClassNotFoundException {
        HashSet<ManagedDevice> devices = new HashSet<>();
        JsonNode root = mapper.readTree(json);
        JsonNode devicesJSON =  root.get("devices");
        for(final JsonNode device:devicesJSON){
            String factoryClass = device.get("factory").asText();
            Optional<AbstractManagedDeviceFactory> foundFactory =  factories.stream()
                    .filter((factory)->factory.getClass().getSimpleName().equals(factoryClass))
                    .findFirst();
            if (foundFactory.isPresent())
                devices.add(foundFactory.get().fromJSON(device));
            else
                throw new ClassNotFoundException("Factory class "+ factoryClass + " not found for device ID " + device.get("name").asText());
        }
        return devices;
    }

    static HashSet<Device> loadSourcesToServerFromJSON(HashSet<? extends Commandable> devices, SourcesServer server, String json) throws IOException, ClassNotFoundException {
        JsonNode root = mapper.readTree(json);
        HashSet<Device> sources = new HashSet<>();
        for(final JsonNode source:root) {
            server.addSource(source,devices);
        }
        return sources;
    }

    static HashSet<Module> loadModulesFromJSON(HashSet<? extends Switchable> switchables, String json) throws IOException, ClassNotFoundException {
        HashSet<Module> modules = new HashSet<>();
        JsonNode root = mapper.readTree(json);
        JsonNode moduleJSON = root.get("modules");
        for (final JsonNode module : moduleJSON) {
            Module.ModuleBuilder builder = Module.newBuilder()
                    .setName(module.get("name").asText());
            JsonNode structure = module.get("sequence");
            int counter = 0;
            for (final JsonNode deviceArray : structure) {
                for (final JsonNode deviceID : deviceArray) {
                    Optional<? extends Switchable> device = switchables.stream()
                            .filter((dev) -> dev.getName().equals(deviceID.asText())).findFirst();
                    if (device.isPresent()) {
                        builder.addSwitchable(counter, device.get().getName(), device.get());
                    }
                    else
                        throw new ClassNotFoundException("Switchable id "+ deviceID.asText() + " not found for module " + module.get("name").asText());
                }
                counter++;
            }
            modules.add(builder.build());
        }
        return modules;
    }

    static HashSet<VisModule> loadVisModulesFromJSON(HashSet<? extends ManagedDevice> devices, String json) throws IOException, ClassNotFoundException {
        HashSet<VisModule> modules = new HashSet<>();
        JsonNode root = mapper.readTree(json);
        JsonNode moduleJSON = root.get("vismodules");
        for (final JsonNode module : moduleJSON) {
            VisModule.VisModuleBuilder builder = VisModule.newBuilder()
                    .setName(module.get("name").asText());
            JsonNode source = module.get("source");
            Optional<? extends ManagedDevice> device = devices.stream()
                    .filter((dev)->dev.getName().equals(source.asText())).findFirst();
            if (device.isPresent())
                builder.setVideoSource(device.get());
            else
                throw new ClassNotFoundException("Device "+ source.asText() + " not found for vis module " + module.get("name").asText());

            JsonNode visuals = module.get("visualisers");
            for (final JsonNode vis : visuals) {
                JsonNode visual = module.get("source");
                Optional<? extends ManagedDevice> visdevice = devices.stream()
                        .filter((dev) -> dev.getName().equals(vis.asText())).findFirst();
                if (visdevice.isPresent()) {
                    builder.addVisualiser(visdevice.get());
                }
                else
                    throw new ClassNotFoundException("Device id "+ vis.asText() + " not found for vis module " + module.get("name").asText());
            }
        modules.add(builder.build());
        }
        return modules;
    }

    static HashSet<SwitchGroup> loadSwitchersFromJSON(HashSet<? extends Switchable> switchables, String json) throws IOException, ClassNotFoundException {
        HashSet<SwitchGroup> switchGroups = new HashSet<>();
        JsonNode root = mapper.readTree(json);
        JsonNode moduleJSON = root.get("switchGroups");
        for (final JsonNode switcher : moduleJSON) {
            SwitchGroup.SwitcherBuilder builder = SwitchGroup.newBuilder()
                    .setName(switcher.get("name").asText());
            JsonNode structure = switcher.get("list");
            for (final JsonNode deviceID : structure) {
                Optional<? extends Switchable> device = switchables.stream()
                        .filter((dev) -> dev.getName().equals(deviceID.asText())).findFirst();
                if (device.isPresent()) {
                    builder.addSwitchable(device.get().getName(), device.get());
                }
                else
                    throw new ClassNotFoundException("Switchable id "+ deviceID.asText() + " not found for switcher " + switcher.get("name").asText());
            }
            switchGroups.add(builder.build());

        }
        return switchGroups;
    }

    static Set<TBot> loadBotsFromJSON(Set<? extends Commandable> commandables, String bots) throws IOException, ClassNotFoundException {
        HashSet<TBot> botsSet = new HashSet<>();
        JsonNode botsNode = mapper.readTree(bots);
        for (JsonNode botNode : botsNode) {
            TBot bot = new TBot(botNode);
            for (JsonNode r : botNode.get("responses")) {
                switch (r.get("type").asText()) {
                    case "choice":
                        ArrayList<String> choice = new ArrayList<>();
                        r.get("choice").forEach((c) -> choice.add(c.asText()));
                        bot.addChoiceResponce(r.get("request").asText(), r.get("text").asText(), choice);
                    break;
                    case "action":
                        Optional<? extends Commandable> cd = commandables.stream().filter(((c) -> c.getName().equals(r.get("device").asText()))).findFirst();
                        if(cd.isPresent()){
                            if (cd.get().getCommand(r.get("command").asText()) != null)
                                bot.addActionResponce(r.get("request").asText(), cd.get().getCommand(r.get("command").asText()));
                            else
                                throw new ClassNotFoundException("Command " + r.get("command")+ " not found for device  "+ r.get("device") + " for bot " + bot.getName());
                        }
                        else
                            throw new ClassNotFoundException("Commandable " + r.get("device") + " not found for bot " + bot.getName() + " for request " + r.get("request").asText());
                    break;
                    case "text":
                        bot.addTextResponce(r.get("request").asText(), r.get("text").asText());
                    break;
                }
                botsSet.add(bot);
            }
        }
        return botsSet;
    }

    static Set<DeviceView> loadDeviceViewsFromJSON(Set<? extends Device> devices,
                                                   String viewTypes, String views, SwitchGroup switchGroup)
                                                                    throws IOException, ClassNotFoundException {
        JsonNode viewTypesNode = mapper.readTree(viewTypes);
        JsonNode viewsNode = mapper.readTree(views).get("devices");
        HashSet<DeviceView> viewsMap = new HashSet<>();

        for (JsonNode view : viewsNode) {
            Optional<? extends Device> deviceOptional = devices.stream().filter((dev)->dev.getName().
                                                                    equals(view.get("name").asText())).findFirst();
            if(!deviceOptional.isPresent())
                continue;
            //throw new ClassNotFoundException("Device view type not found:" + view.get("name").asText());
            Device device = deviceOptional.get();

            Optional<Switchable> toDisable = switchGroup.getChildren().stream().filter((c)->c == device).findFirst();
            if (!toDisable.isPresent())
                toDisable = switchGroup.getChildren().stream().filter((c)->c instanceof Module).
                                             filter((x)->x.getAllChildren().contains(device)).findFirst();
            Runnable disableF;
            Runnable enableF;

            if (toDisable.isPresent()){
                Switchable toDisableSw = toDisable.get();
                disableF = ()->switchGroup.addExcludedCmd().exec(toDisableSw);
                enableF = ()->switchGroup.removeExcludedCmd().exec(toDisableSw);
            }
            else {
                disableF = () -> { };
                enableF = () -> { };

            }
            viewsMap.add(new DeviceView(device,view,viewTypesNode,disableF,enableF));
        }
        return viewsMap;
    }

    static void updateDisabledFromJSON(String disabled, SwitchGroup switchGroup) throws IOException {
        JsonNode disabledNode = mapper.readTree(disabled);
        for (JsonNode record : disabledNode) {
            String group = record.get("switchGroup").asText();
            if(!group.equals(switchGroup.getName()))
                continue;
            for (JsonNode view : record.get("list")) {
                switchGroup.getChildren().stream()
                        .filter((dev)-> dev.getName().equals(view.asText()))
                        .findFirst().ifPresent((dev)->switchGroup.addExcludedCmd().exec(dev));
            }
        }
    }

    static void saveDisabledDataToJSON(HashSet<SwitchGroup> switchGroups, File file) throws IOException {
        ArrayNode root = mapper.createArrayNode();
        switchGroups.forEach((switchGroup -> {
            ObjectNode switchGroupJSON = mapper.createObjectNode();
            switchGroupJSON.put("switchGroup",switchGroup.getName());
            ArrayNode list = mapper.createArrayNode();
            switchGroup.getDisabledSwitchables().forEach((switchable -> {
                list.add(switchable.getName());
            }));
            switchGroupJSON.put("list",list);
            root.add(switchGroupJSON);
        }));

        ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        writer.writeValue(file, root);

    }


}
