package org.gmig.gecs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gmig.gecs.device.Device;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.device.Switchable;
import org.gmig.gecs.factories.ManagedDeviceFactory;
import org.gmig.gecs.groups.Module;
import org.gmig.gecs.groups.SwitchGroup;
import org.gmig.gecs.views.DeviceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by brix on 4/23/2018.
 */
public final class StructureReader {

    public static final HashSet<ManagedDeviceFactory> factories = new HashSet<>();
    public static final ObjectMapper mapper = new ObjectMapper();

    public static HashSet<ManagedDevice> loadDevicesFromJSON(String json) throws IOException, IllegalArgumentException {
        HashSet<ManagedDevice> devices = new HashSet<>();
        JsonNode root = mapper.readTree(json);
        JsonNode devicesJSON =  root.get("devices");
        for(final JsonNode device:devicesJSON){
            String factoryClass = device.get("factory").asText();
            Optional<ManagedDeviceFactory> foundFactory =  factories.stream()
                    .filter((factory)->factory.getClass().getSimpleName().equals(factoryClass))
                    .findFirst();
            if (foundFactory.isPresent())
                devices.add(foundFactory.get().fromJSON(device));
            else
                throw new IllegalArgumentException("Factory class "+ factoryClass + " not found for device ID" + device.get("name").asText());
        }
        return devices;
    }

    public static HashSet<Device> loadSourcesFromJSON(HashSet<? extends Device> devices, SourcesServer server, String json) throws IOException, IllegalArgumentException {
        JsonNode root = mapper.readTree(json);
        HashSet<Device> sources = new HashSet<>();
        for(final JsonNode source:root) {
            String name = source.get("name").asText();
            String signal = source.get("signal").asText();
            ArrayList<Consumer<?>> commands = new ArrayList<>();
//            for (final JsonNode action : Source.get("actions")) {
//                String deviceName = action.get("device").asText();
//                String command = action.get("command").asText();
//                Optional<? extends Device> deviceO = devices.stream().filter((dev) -> dev.getName().equals(deviceName)).findFirst();
//                if (deviceO.isPresent()) {
//                    Device device = deviceO.get();
//                    try {
//                        commands.add((o) -> device.getCommand(command).exec());
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                    //acceptor.addRule(name,signal,(o)->deviceO.get().getCommand(command).exec());
//                } else
//                    throw new IllegalArgumentException("Device " + deviceName + " not found for Source" + name + " signal " + signal);
//            }
            server.addSource(source,devices);
        }
        return sources;
    }

    public static HashSet<Module> loadModulesFromJSON(HashSet<? extends Switchable> switchables, String json) throws IOException {
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
                        throw new IllegalArgumentException("Switchable id "+ deviceID.asText() + " not found for module " + module.get("name").asText());

                }
                counter++;
            }
            modules.add(builder.build());
        }
        return modules;
    }
    public static HashSet<SwitchGroup> loadSwitchersFromJSON(HashSet<? extends Switchable> switchables, String json) throws IOException {
        HashSet<SwitchGroup> switchGroups = new HashSet<>();
        JsonNode root = mapper.readTree(json);
        JsonNode moduleJSON = root.get("switchGroups");
        if(moduleJSON == null) return null;
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
                    throw new IllegalArgumentException("Switchable id "+ deviceID.asText() + " not found for switcher " + switcher.get("name").asText());
            }
            switchGroups.add(builder.build());

        }
        return switchGroups;
    }
    public static Set<DeviceView> loadDeviceViewsFromJSON(Set<? extends Device> devices, String viewTypes, String views) throws IOException, ClassNotFoundException {
        JsonNode viewTypesNode = mapper.readTree(viewTypes);
        JsonNode viewsNode = mapper.readTree(views).get("devices");
        HashSet<DeviceView> viewsMap = new HashSet<>();

        for (JsonNode view : viewsNode) {
            Optional<? extends Device> deviceOptional = devices.stream().filter((dev)->dev.getName().equals(view.get("name").asText())).findFirst();
            if(!deviceOptional.isPresent())
                continue;
            //throw new ClassNotFoundException("Device view type not found:" + view.get("name").asText());
            Device device = deviceOptional.get();
            viewsMap.add(new DeviceView(device,view,viewTypesNode));
        }
        return viewsMap;
    }



    }
