package org.gmig.gecs.command;

import org.apache.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Created by brix isOn 3/7/2018.
 */



public class ListenableArgCommand<T>  {
    private static final Logger logger = Logger.getLogger(ListenableArgCommand.class);

    protected final ArgCommand<Object,T> command;
    protected final String name;
    protected final Executor executor = Executors.newCachedThreadPool();
    public final ConcurrentLinkedQueue<Runnable> started                     = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue <BiConsumer<T,Throwable>> completed    = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<Consumer<T>> success                  = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue <Consumer<Throwable>> exception        = new ConcurrentLinkedQueue<>();

    public String getName() {
        return name;
    }

    public ListenableArgCommand(ArgCommand<Object,T> command, String name) {
        this.command = command;
        this.name = name;
        success.add((o)->logger.info(name + ":completed successfully with:" + o));
        exception.add((t)->logger.info(name + ":completed exceptionally with:" + t));
        started.add(()->logger.debug(name + ":begin"));
    }


    public synchronized CompletableFuture<T> exec(Object arg) {
        started.forEach((e) -> executor.execute(e));
        CompletableFuture<T> f = command.apply(arg);
        completed.forEach((e) ->
                f.whenCompleteAsync(e, executor));
        success.forEach((e) ->
                f.handleAsync((T o, Throwable ex) -> {
                    if (ex == null)
                        e.accept(o);
                    return null;
                }, executor));
        exception.forEach((e) ->
                f.handleAsync((o, ex) -> {
                    if (ex != null)
                        e.accept(ex);
                    return null;
                }, executor));
        return f;
    }

    public Command<T> getCommand(Object arg){
        return ()->command.apply(arg);
    }
}



