package org.gmig.gecs.command;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;

/**
 * Created by brix isOn 3/7/2018.
 */



public class ListenableCommand<T> extends ListenableArgCommand<T> implements Serializable {
    //private static final Logger logger = Logger.getLogger(ListenableCommand.class);

//    protected Command<T> command;
//
//    public String getName() {
//        return name;
//    }

//    protected String name;
//    protected Executor executor = Executors.newCachedThreadPool();
//    public final ConcurrentLinkedQueue<Runnable> started                     = new ConcurrentLinkedQueue<>();
//    public final ConcurrentLinkedQueue <BiConsumer<T,Throwable>> completed    = new ConcurrentLinkedQueue<>();
//    public final ConcurrentLinkedQueue<Consumer<T>> success                  = new ConcurrentLinkedQueue<>();
//    public final ConcurrentLinkedQueue <Consumer<Throwable>> exception        = new ConcurrentLinkedQueue<>();

    public ListenableCommand(Command<T> command, String name) {
        super((o) -> command.get(), name);
//        this.command = command;
//        this.name = name;
//        success.add((o)->logger.info(name + ":completed successfully with:" + o));
//        exception.add((t)->logger.info(name + ":completed exceptionally with:" + t));
//        started.add(()->logger.debug(name + ":begin"));
   }


   public synchronized CompletableFuture<T> exec() {
       return super.exec(null);
   }
//        started.forEach((e) -> executor.execute(e));
//        CompletableFuture<T> f = command.get();
//        completed.forEach((e) ->
//                f.whenCompleteAsync(e, executor));
//        success.forEach((e) ->
//                f.handleAsync((T o, Throwable ex) -> {
//                    if (ex == null)
//                        e.accept(o);
//                    return null;
//                }, executor));
//        exception.forEach((e) ->
//                f.handleAsync((o, ex) -> {
//                    if (ex != null)
//                        e.accept(ex);
//                    return null;
//                }, executor));
//        return f;
//    }
//
   public Command<T> getCommand(){
        return this::exec;
    }
}



