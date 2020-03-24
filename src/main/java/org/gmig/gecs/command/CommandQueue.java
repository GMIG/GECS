package org.gmig.gecs.command;

import java.util.concurrent.*;

/**
 * Created by brix isOn 2/27/2018.
 */
public class CommandQueue {
    private CompletableFuture<?> last = CompletableFuture.completedFuture(null);
    private final LinkedBlockingQueue <Command<?>> queue = new LinkedBlockingQueue<>();

    public void setCommandTimeout(int commandTimeout) {
        this.commandTimeout = commandTimeout;
    }
    public final ConcurrentLinkedQueue<Runnable> queueEmpty = new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<Runnable> queueNotEmpty = new ConcurrentLinkedQueue<>();

    private int commandTimeout = 5*60*1000;


    private final ScheduledExecutorService timoutListener = Executors.newSingleThreadScheduledExecutor();

    private synchronized void callbackOnDone (){
            Command next = queue.poll();
            if (next != null) {
                execute(next);
            } else {
                queueEmpty.forEach(Runnable::run);
            }
    }

    private synchronized void execute(Command<?> cmd) {
        CompletableFuture<?> future = cmd.get();
        future.whenComplete((o,t)->callbackOnDone());
        last = future;
    }

    public synchronized void clear() {
        queue.clear();
    }

    public synchronized <T>CompletableFuture<T> add(Command<T> cmd){
        queueNotEmpty.forEach(Runnable::run);
        CompletableFuture<T> commandFuture= new CompletableFuture<>();
        Command<T> cmdWithFutureCallback = ()-> {
            CompletableFuture<T> future = cmd.get();
            timoutListener.schedule(()->{
                //commandFuture.completeExceptionally(new Throwable("Command queue:Command timeout"));
                future.completeExceptionally(new Throwable("Command queue:Command timeout"));
            },commandTimeout, TimeUnit.MILLISECONDS);
            future.whenComplete((o,t)->{
                if (t == null)
                    commandFuture.complete(o);
                else
                    commandFuture.completeExceptionally(t);
            });
            return future;
        };
        if (last.isDone()) {
            execute(cmdWithFutureCallback);
        }
        else
            queue.add(cmdWithFutureCallback);
        return commandFuture;
    }

    public <T>Command<T> getCommand(Command<T> cmd){
        return ()->add(cmd);
    }

    public <U,T>ArgCommand<U,T> getArgCommand(ArgCommand<U,T> cmd){
        return (U arg)->add(()->cmd.apply(arg));
    }

}
