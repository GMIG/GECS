package org.gmig.gecs.groups;

import org.gmig.gecs.command.Command;
import org.gmig.gecs.command.ComplexCommandBuilder;
import org.gmig.gecs.command.ListenableCommand;
import org.gmig.gecs.device.Switchable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by brix on 4/17/2018.
 */
public class Module implements Switchable {

    private final LinkedHashMap<Integer,HashMap<String,Switchable>> moduleStructure;
    private final ListenableCommand<?> switchOn;
    private final ListenableCommand<?> switchOff;
    private final String ID;
    //private final String Description;


    private Module(ModuleBuilder b){
        this.moduleStructure = b.moduleStructure;
        this.ID = b.ID;
        this.switchOn = new ListenableCommand<>(b.switchOn,"module:" + ID + ":switchOn");
        this.switchOff = new ListenableCommand<>(b.switchOff,"module:" + ID + ":switchOff");
    }

    public static ModuleBuilder newBuilder(){return new ModuleBuilder();}

    public static class ModuleBuilder {
        private final LinkedHashMap<Integer,HashMap<String,Switchable>> moduleStructure = new LinkedHashMap<>();
        ComplexCommandBuilder swithcOncmdbuilder = ComplexCommandBuilder.builder();
        ComplexCommandBuilder swithcOffcmdbuilder = ComplexCommandBuilder.builder();
        private String ID ;

        private Command<?> switchOn;
        private Command<?> switchOff;

        public ModuleBuilder addSwitchable(int where, String id, Switchable sw) {
            if (!moduleStructure.containsKey(where)) {
                moduleStructure.put(where, new HashMap<>());
            }
            moduleStructure.get(where).put(id, sw);
            return this;
        }

        public ModuleBuilder setName(String ID) {
            this.ID = ID;
            return this;
        }

        public Module build(){
            String ID = "";

                for (int i = 0; i < moduleStructure.keySet().size(); i++) {
                    HashMap<String, Switchable> parallel = moduleStructure.get(i);
                    for (Map.Entry<String, Switchable> e : parallel.entrySet()) {
                        swithcOncmdbuilder.addCommand(i, e.getKey(), e.getValue().switchOnCmd().getCommand());
                        swithcOffcmdbuilder.addCommand(moduleStructure.keySet().size() - i - 1, e.getKey(), e.getValue().switchOffCmd().getCommand());
                    }
                    ID += parallel.values().stream()
                            .map(Switchable::getName)
                            .collect(Collectors.joining(","));
                    if(i < moduleStructure.keySet().size()-1)
                        ID += ";";
                }
                if(this.ID==null) {
                    this.ID = ID;
                }
                switchOn = swithcOncmdbuilder.build();
                switchOff = swithcOffcmdbuilder.build();

            return new Module(this);
        }
    }

    @Override
    public ListenableCommand<?> switchOnCmd() {
        return switchOn;
    }

    @Override
    public ListenableCommand<?> switchOffCmd() {
        return switchOff;
    }

    @Override
    public String getName() {
        return ID;
    }

    @Override
    public Set<Switchable> getChildren() {
        Set<Switchable> children = moduleStructure
                .values()
                .stream().flatMap((i)->i.values().stream())
                .collect(Collectors.toSet());

        return children;
    }

}
