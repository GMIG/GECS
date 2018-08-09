package org.gmig.gecs.command;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Created by brix isOn 3/2/2018.
 */
//@FunctionalInterface
public interface Command <T> extends Supplier<CompletableFuture<T>> {

    default Command<T> thenWait(int millis){
        return ()->{
            CompletableFuture<T> newF = new CompletableFuture<>();
            get().handle((T o, Throwable ex) -> {
                    Executors.newSingleThreadScheduledExecutor().schedule( ()->{
                        if (ex != null)
                            newF.completeExceptionally(ex);
                        else
                            newF.complete(o);
                        },
                            millis,
                            TimeUnit.MILLISECONDS);
                return null;
            });
            return newF;
        };
    }
}
