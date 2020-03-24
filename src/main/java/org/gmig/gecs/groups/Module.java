package org.gmig.gecs.groups;

import org.gmig.gecs.command.Command;
import org.gmig.gecs.command.ComplexCommandBuilder;
import org.gmig.gecs.command.ListenableCommand;
import org.gmig.gecs.device.StandardCommands;
import org.gmig.gecs.device.Switchable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by brix on 4/17/2018.
 */
public class Module implements Switchable {

    protected final LinkedHashMap<Integer,HashMap<String,Switchable>> moduleStructure;
    private final ListenableCommand<?> switchOn;
    private final ListenableCommand<?> switchOff;
    private final ListenableCommand<?> forceSwitchOff;

    private final String ID;
    //private final String Description;


    protected Module(ModuleBuilder b){
        this.moduleStructure = b.moduleStructure;
        this.ID = b.ID;
        this.switchOn = new ListenableCommand<>(b.switchOn,"module:" + ID + ":switchOn");
        this.switchOff = new ListenableCommand<>(b.switchOff,"module:" + ID + ":switchOff");
        this.forceSwitchOff = new ListenableCommand<>(b.forceSwitchOff,"module:" + ID + ":switchOff");
    }

    public static ModuleBuilder newBuilder(){return new ModuleBuilder();}

    public static class ModuleBuilder <T extends ModuleBuilder<T>>{
        private final LinkedHashMap<Integer,HashMap<String,Switchable>> moduleStructure = new LinkedHashMap<>();
        final ComplexCommandBuilder swithcOncmdbuilder = ComplexCommandBuilder.builder();
        final ComplexCommandBuilder swithcOffcmdbuilder = ComplexCommandBuilder.builder();
        final ComplexCommandBuilder forceSwithcOffcmdbuilder = ComplexCommandBuilder.builder();

        private String ID ;

        private Command<?> switchOn;
        private Command<?> switchOff;
        private Command<?> forceSwitchOff;

        public T addSwitchable(int where, String id, Switchable sw) {
            if (!moduleStructure.containsKey(where)) {
                moduleStructure.put(where, new HashMap<>());
            }
            moduleStructure.get(where).put(id, sw);
            return (T)this;
        }

        public T setName(String ID) {
            this.ID = ID;
            return (T)this;
        }

        public Module build(){
            String ID = "";
            for (int i = 0; i < moduleStructure.keySet().size(); i++) {
                HashMap<String, Switchable> parallel = moduleStructure.get(i);
                for (Map.Entry<String, Switchable> e : parallel.entrySet()) {
                    swithcOncmdbuilder.addCommand(i, e.getKey(), e.getValue().switchOnCmd().getCommand());
                    swithcOffcmdbuilder.addCommand(moduleStructure.keySet().size() - i - 1, e.getKey(), e.getValue().switchOffCmd().getCommand());
                    forceSwithcOffcmdbuilder.addCommand(0,e.getKey(), e.getValue().switchOffCmd().getCommand());
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
            forceSwitchOff = forceSwithcOffcmdbuilder.collect(0);
            return new Module(this);
        }
    }

    public ListenableCommand<?> forceSwitchOffCmd() {
        return forceSwitchOff;
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
    public ListenableCommand<?> getCommand(String ID) {
        if(ID.equals(StandardCommands.switchOff.name()))
            return switchOff;
        if(ID.equals(StandardCommands.switchOn.name()))
            return switchOn;
        if(ID.equals("forceSwitchOff"))
            return forceSwitchOff;

        return null;
    }

    @Override
    public String getName() {
        return ID;
    }

    @Override
    public Set<Switchable> getChildren() {

        return moduleStructure
                .values()
                .stream().flatMap((i)->i.values().stream())
                .collect(Collectors.toSet());
    }

}
