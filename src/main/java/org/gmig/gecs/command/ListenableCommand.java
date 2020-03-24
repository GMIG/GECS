package org.gmig.gecs.command;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

/**
 * Created by brix isOn 3/7/2018.
 */



public class ListenableCommand<T> extends ListenableArgCommand<T> implements Serializable {
    public ListenableCommand(Command<T> command, String name) {
        super((o) -> command.get(), name);
   }


   public synchronized CompletableFuture<T> exec() {
       return super.exec(null);
   }
   public Command<T> getCommand(){
        return this::exec;
    }
}



