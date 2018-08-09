package org.gmig.gecs.device;

import org.gmig.gecs.command.ArgCommand;
import org.gmig.gecs.command.ListenableCommand;

import java.util.concurrent.CompletableFuture;

/**
 * Created by brix isOn 3/16/2018.
 */
public class Source extends Device {


    public static SourceBuilder newBuilder(){
        return new SourceBuilder();
    }

    public static class SourceBuilder extends DeviceBuilder<SourceBuilder>{
        private SourceBuilder() {}



        public <U> SourceBuilder addSignal(String s) {
            super.addArgCommand(s, (arg) -> CompletableFuture.completedFuture(s));
            return this;
        }

        public<V> SourceBuilder addAction(String signal, Device d, String cmdName) {
            super.argCommands.get(signal).success.add((arg)->d.getArgCommand(cmdName).exec(arg));
            return this;
        }

        @Override
        public Source build() throws IllegalArgumentException{
            if(ID==null)
                throw new IllegalArgumentException("Device name not set. Use setName()");
            return new Source(this);
        }
    }
    public ListenableCommand<StateRequestResult> stateReq()
        {return (ListenableCommand<StateRequestResult>)getCommand(StandardCommands.init.name());}
    public ListenableCommand<?> checkCmd()
        {return  getCommand(StandardCommands.check.name());}

   protected Source(SourceBuilder builder) {
        super(builder);
    }
    public static class Signal implements ArgCommand{



        @Override
        public Object apply(Object o) {
            return CompletableFuture.completedFuture(o);
        }


    }
}
