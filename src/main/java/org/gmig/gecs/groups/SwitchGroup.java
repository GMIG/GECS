package org.gmig.gecs.groups;

import org.gmig.gecs.command.Command;
import org.gmig.gecs.command.ComplexCommandBuilder;
import org.gmig.gecs.command.ListenableCommand;
import org.gmig.gecs.device.StandardCommands;
import org.gmig.gecs.device.Switchable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by brix on 4/24/2018.
 */
public class SwitchGroup implements Switchable<HashMap<String,?>,HashMap<String,?>>,Serializable {

    public HashMap<String, Switchable> getStructure() {
        return structure;
    }

    private final HashMap<String,Switchable> structure;
    private final ListenableCommand<HashMap<String,?>> switchOn;
    private final ListenableCommand<HashMap<String,?>> switchOff;
    private final String ID;

    public static SwitcherBuilder newBuilder(){return new SwitcherBuilder();}
    private SwitchGroup(SwitcherBuilder b) {
        this.structure = b.structure;
        this.ID = b.ID;
        this.switchOn = new ListenableCommand<>(b.switchOn, "switchGroup:" + ID + ":"+ StandardCommands.switchOn.name() + ":" + ID );
        this.switchOff = new ListenableCommand<>(b.switchOff, "switchGroup:"+ ID + ":" + StandardCommands.switchOff.name() + ":"+ ID );
    }
    public static class SwitcherBuilder {
        private final HashMap<String,Switchable> structure = new LinkedHashMap<>();
        ComplexCommandBuilder swithcOncmdbuilder = ComplexCommandBuilder.builder();
        ComplexCommandBuilder swithcOffcmdbuilder = ComplexCommandBuilder.builder();

        private String ID ;

        private Command<HashMap<String,?>> switchOn;
        private Command<HashMap<String,?>> switchOff;
        public SwitcherBuilder addSwitchable(String id, Switchable sw) {
            structure.put(id, sw);
            return this;
        }
        public SwitcherBuilder setName(String ID) {
            this.ID = ID;
            return this;
        }

        public SwitchGroup build() {
            String ID = "";

            for (Map.Entry<String, Switchable> e : structure.entrySet()) {
                swithcOncmdbuilder.addCommand(0,e.getKey(),e.getValue().switchOnCmd().getCommand());
                swithcOffcmdbuilder.addCommand(0,e.getKey(),e.getValue().switchOffCmd().getCommand());
            }
            ID += structure.values().stream().map(Switchable::getName).collect(Collectors.joining(","));
            ID += ";";
            if (this.ID==null) {
                this.ID = ID;
            }
            switchOn = swithcOncmdbuilder.parallel(0);
            switchOff = swithcOffcmdbuilder.parallel(0);
            return new SwitchGroup(this);
        }
    }


    @Override
    public ListenableCommand<HashMap<String,?>> switchOnCmd() {
        return switchOn;
    }

    @Override
    public ListenableCommand<HashMap<String,?>> switchOffCmd() {
        return switchOff;
    }

    @Override
    public String getName() {
        return ID;
    }
    @Override
    public Set<Switchable> getChildren() {
        return structure
                .values()
                .stream()
                .collect(Collectors.toSet());
    }




}
