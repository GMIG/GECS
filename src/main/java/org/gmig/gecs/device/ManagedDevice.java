package org.gmig.gecs.device;

import org.gmig.gecs.command.ArgCommand;
import org.gmig.gecs.command.Command;
import org.gmig.gecs.command.CommandQueue;
import org.gmig.gecs.command.ListenableCommand;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by brix isOn 3/16/2018.
 */
public class ManagedDevice extends Device implements Switchable{
    final public RegularCheckManager manager;
    final public CommandQueue queue;

    public static ManagedDeviceBuilder newBuilder(){
        return new ManagedDeviceBuilder();
    }

    public static class ManagedDeviceBuilder extends DeviceBuilder<ManagedDeviceBuilder>{
        final CommandQueue queue;
        int checkTime = 3*60*1000;
        private ManagedDeviceBuilder() {
            queue = new CommandQueue();
            setData("command queue",queue);
        }
        //private SourceDeviceBuilder(CommandQueue queue) {
        //    this.queue = queue;
        //}

        @Override
        public <U> ManagedDeviceBuilder addCommand(String s, Command<U> cmd) {
            super.addCommand(s, queue.getCommand(cmd));
            return this;
        }
        @Override
        public<V> ManagedDeviceBuilder addArgCommand(String s, ArgCommand<Object,V> cmd) {
            super.addArgCommand(s, queue.getArgCommand(cmd));
            return this;
        }

        public  <T>ManagedDeviceBuilder setSwitchOnCommand(Command<T> cmdTurnOn) {
           addCommand(StandardCommands.switchOn.name(), cmdTurnOn);
            return this;
        }

        public <T>ManagedDeviceBuilder setSwitchOffCommand(Command<T> cmdTurnOff) {
            addCommand(StandardCommands.switchOff.name(), cmdTurnOff);
            return this;
        }

        public <T>ManagedDeviceBuilder setCheckCommand(Command<T> cmdCheck) {
            addCommand(StandardCommands.check.name(), cmdCheck);
            return this;
        }

        public <T>ManagedDeviceBuilder setCheckedRestartCommand(Command<T> cmdCRestart) {
            addCommand(StandardCommands.checkedRestart.name(), cmdCRestart);
            return this;
        }

        public ManagedDeviceBuilder setStateRequestCommand(Command<StateRequestResult> cmdInit) {
            addCommand(StandardCommands.init.name(),cmdInit);
            return this;
        }

        public ManagedDeviceBuilder setCheckResendMillis(int millis) {
            checkTime = millis;
            return this;
        }

        public ManagedDeviceBuilder setCheckResendTimeSeconds(int millis) {
            setCheckResendMillis(millis*1000);
            return this;
        }

        public ManagedDeviceBuilder setCheckResendTimeMinutes(int millis) {
            setCheckResendMillis(millis*60*1000);
            return this;
        }

        @Override
        public ManagedDevice build() throws IllegalArgumentException{
            if(ID==null)
                throw new IllegalArgumentException("Device name not set. Use setName()");
            return new ManagedDevice(this);
        }
    }
    public ListenableCommand<StateRequestResult> stateReq()
        {return (ListenableCommand<StateRequestResult>)getCommand(StandardCommands.init.name());}
    public ListenableCommand<?> checkCmd()
        {return  getCommand(StandardCommands.check.name());}
    public ListenableCommand<?> checkedRestartCmd()
        {return  getCommand(StandardCommands.checkedRestart.name());}

    @Override
    public ListenableCommand<?> switchOnCmd()
        {return getCommand(StandardCommands.switchOn.name());}
    @Override
    public ListenableCommand<?> switchOffCmd()
        {return getCommand(StandardCommands.switchOff.name());}

    @Override
    public Set<Switchable> getChildren() {
        return new HashSet<>();
    }

    protected ManagedDevice(ManagedDeviceBuilder builder) {
        super(builder);
        this.queue = builder.queue;
        this.manager =  new RegularCheckManager(this);
        manager.setDelayMillis(builder.checkTime);
    }
}
