package org.gmig.gecs.device;

import org.gmig.gecs.command.ArgCommand;
import org.gmig.gecs.command.Command;
import org.gmig.gecs.command.ListenableArgCommand;
import org.gmig.gecs.command.ListenableCommand;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by brix isOn 3/16/2018.
 */
@SuppressWarnings("WeakerAccess")
public class Device implements Commandable{
    private HashMap<String, ListenableCommand<?>> commands = new HashMap<>();
    private HashMap<String, ListenableArgCommand<?>> argCommands = new HashMap<>();

    public Object getData(String key) {
        return data.get(key);
    }

    public Map<String, Object> getDataList() {
        return Collections.unmodifiableMap(data);
    }

    private HashMap<String, Object> data = new HashMap<>();

    private final String ID;
    private final Class<?> factory;
    private final String description;

    public Class<?> getFactoryType() {return factory;}
    public String getDescription() { return description;}
    public String getName() {
        return ID;
    }

    protected Device(DeviceBuilder b) {
        this.commands = b.commands;
        this.argCommands = b.argCommands;
        this.ID = b.ID;
        this.description = b.description;
        this.factory = b.factory;
        this.data = b.data;
    }


    public static DeviceBuilder builder(){
        return new DeviceBuilder();
    }
    public ListenableCommand<?> getCommand(String ID) {
        return commands.get(ID);
    }
    public ListenableArgCommand<?> getArgCommand(String ID) {
        return argCommands.get(ID);
    }


    public Map<String, ? extends ListenableCommand<?>> getCommandList() {
        return Collections.unmodifiableMap(commands);
    }

    public Map<String, ? extends ListenableArgCommand<?>> getArgCommandList() {
        HashMap<String,ListenableArgCommand<?>> map3 = new HashMap<>();
        map3.putAll(commands);
        map3.putAll(argCommands);
        return Collections.unmodifiableMap(map3);
    }

    @SuppressWarnings("WeakerAccess")
    public static class DeviceBuilder <T extends DeviceBuilder<T>>{
        protected  Class<?> factory;
        protected  String description;
        protected String ID;

        protected final HashMap<String,ListenableCommand<?>> commands = new HashMap<>();
        protected final HashMap<String, ListenableArgCommand<?>> argCommands = new HashMap<>();
        private final HashMap<String, Object> data = new HashMap<>();

        public<U> T addCommand(String s, Command<U> cmd) {
            commands.put(s, new ListenableCommand<>(cmd,ID + ":" + s));
            return (T) this;
        }

        public<V> T addArgCommand(String s, ArgCommand<Object,V> cmd) {
            argCommands.put(s, new ListenableArgCommand<>(cmd,ID + ":" + s));
            return (T) this;
        }
       // @JsonSetter("name")
        public T setName(String s) {
            this.ID  = s;
            return (T) this;
        }
        //@JsonSetter("description")
        public T setDescription(String s) {
            this.description  = s;
            return (T) this;
        }
        public T setFactory(Class<?> s) {
            this.factory = s;
            return (T) this;
        }

        public T setData(String key,Object s) {
            this.data.put(key,s);
            return (T) this;
        }


        public Device build() throws IllegalArgumentException{
            if(ID==null)
                throw new IllegalArgumentException("Device name not set. Use setName()");
            return new Device(this);
        }
    }
}
