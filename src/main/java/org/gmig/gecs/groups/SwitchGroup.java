package org.gmig.gecs.groups;

import org.gmig.gecs.command.Command;
import org.gmig.gecs.command.ComplexCommandBuilder;
import org.gmig.gecs.command.ListenableArgCommand;
import org.gmig.gecs.command.ListenableCommand;
import org.gmig.gecs.device.StandardCommands;
import org.gmig.gecs.device.Switchable;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Created by brix on 4/24/2018.
 */
public class SwitchGroup implements Switchable<HashMap<String,?>,HashMap<String,?>>,Serializable {

    public HashMap<String, Switchable> getStructure() {
        return structure;
    }

    public Set<Switchable> getDisabledSwitchables() {
        return java.util.Collections.unmodifiableSet(excludedSwitchables);
    }
    private final HashMap<String,Switchable> structure;
    private final Set<Switchable> excludedSwitchables;

    private final ListenableCommand<HashMap<String,?>> switchOn;
    private final ListenableCommand<HashMap<String,?>> switchOff;
    private final ListenableArgCommand<?> addExcluded;
    private final ListenableArgCommand<?> removeExcluded;

    private final String ID;

    public static SwitcherBuilder newBuilder(){return new SwitcherBuilder();}
    private SwitchGroup(SwitcherBuilder b) {
        this.structure = b.structure;
        this.excludedSwitchables = b.excludedSwitchables;
        this.addExcluded = new ListenableArgCommand<Switchable>((added)->{
            Switchable addedSw = (Switchable)added;
            this.excludedSwitchables.add(addedSw);
            return CompletableFuture.completedFuture(addedSw);
        },"switchGroup:" + b.ID + ":added exclusion");
        this.removeExcluded = new ListenableArgCommand<Switchable>((removed)->{
            Switchable removedSw = (Switchable)removed;
            this.excludedSwitchables.remove(removedSw);
            return CompletableFuture.completedFuture(removedSw);
        },"switchGroup:" + b.ID + ":removed exclusion");
        //this.removeExcluded
        this.ID = b.ID;
        this.switchOn = new ListenableCommand<>(b.switchOn, "switchGroup:" + ID + ":"+ StandardCommands.switchOn.name() + ":" + ID );
        this.switchOff = new ListenableCommand<>(b.switchOff, "switchGroup:"+ ID + ":" + StandardCommands.switchOff.name() + ":"+ ID );
    }
    public static class SwitcherBuilder {
        private final HashMap<String,Switchable> structure = new LinkedHashMap<>();
        private final Set<Switchable> excludedSwitchables = Collections.synchronizedSet(new HashSet<>());

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
                Command<?> conditionalSwitchOn = ()->{
                    if (!excludedSwitchables.contains(e.getValue())){
                        return e.getValue().switchOnCmd().exec();
                    }
                    return CompletableFuture.completedFuture(null);
                };
                Command<?> conditionalSwitchOff = ()->{
                    if (!excludedSwitchables.contains(e.getValue())){
                        return e.getValue().switchOffCmd().exec();
                    }
                    return CompletableFuture.completedFuture(null);
                };
                swithcOncmdbuilder.addCommand(0,e.getKey(),conditionalSwitchOn);
                swithcOffcmdbuilder.addCommand(0,e.getKey(),conditionalSwitchOff);

                //swithcOncmdbuilder.addCommand(0,e.getKey(),e.getValue().switchOnCmd().getCommand());
                //swithcOffcmdbuilder.addCommand(0,e.getKey(),e.getValue().switchOffCmd().getCommand());
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

    public ListenableArgCommand<?> addExcludedCmd() { return addExcluded;}
    public ListenableArgCommand<?> removeExcludedCmd() { return removeExcluded;}

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

    @Override
    public ListenableCommand<?> getCommand(String ID) {
        if(ID.equals(StandardCommands.switchOff.name()))
            return switchOff;
        if(ID.equals(StandardCommands.switchOn.name()))
            return switchOn;
        return null;
    }



}
