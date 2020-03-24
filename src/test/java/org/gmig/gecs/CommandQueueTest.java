package org.gmig.gecs;

import org.apache.log4j.Logger;
import org.gmig.gecs.command.Command;
import org.gmig.gecs.command.CommandQueue;
import org.gmig.gecs.command.ListenableCommand;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by brix isOn 2/27/2018.
 */
public class CommandQueueTest  {
    private static final Logger logger = Logger.getLogger(CommandQueueTest.class);

    public void testCommand() throws InterruptedException {
        CommandQueue queue = new CommandQueue();
        queue.add(()-> CompletableFuture.supplyAsync(()-> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.debug("DONE 1");
            return null;
        }));
        queue.add(()-> CompletableFuture.supplyAsync(()-> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.debug("DONE 2");
            return null;
        }));

        Thread.sleep(4000);
    }
    public void testCommand1() throws InterruptedException {
        CommandQueue queue = new CommandQueue();
        Command<String> cmd1 = ()-> CompletableFuture.supplyAsync(()-> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.debug("DONE 1");
            return null;
        });
        Command<String> cmd2 = ()-> CompletableFuture.supplyAsync(()-> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.debug("DONE 2");
            return null;
        });
        //cmd1.whenComplete((a,t)->logger.debug(a));
        queue.add(cmd1);
        queue.add(cmd2);

        Thread.sleep(4000);
    }

    public void testBlockingCommand() throws InterruptedException {
        CommandQueue queue = new CommandQueue();
        queue.add(()->  {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.debug("DONE 1");
            return CompletableFuture.completedFuture(null);
        });
        queue.add(()->{logger.debug("Done 2");return CompletableFuture.completedFuture(null);});
        queue.add(()->{logger.debug("Done 3");return CompletableFuture.completedFuture(null);});
        queue.add(()->{logger.debug("Done 4");return CompletableFuture.completedFuture(null);});

        Thread.sleep(3000);
    }

    public void testNeverCompletedCommand() throws InterruptedException {
        CommandQueue queue = new CommandQueue();
        queue.setCommandTimeout(2000);
        queue.add(()->  {
            logger.debug("I will never end");
            return new CompletableFuture<>();
        }).whenComplete((o,t)->logger.debug(t));
        queue.add(()->{logger.debug("Done 2");return CompletableFuture.completedFuture(null);});
        queue.add(()->{logger.debug("Done 3");return CompletableFuture.completedFuture(null);});
        queue.add(()->{logger.debug("Done 4");return CompletableFuture.completedFuture(null);});

        Thread.sleep(3000);
    }

    public void testError() throws InterruptedException {
        CommandQueue queue = new CommandQueue();
        queue.add(()->  {
            CompletableFuture<String> f = new CompletableFuture<>();
            CompletableFuture.supplyAsync(()->{
                try {Thread.sleep(2000);} catch (InterruptedException e) {}
                f.completeExceptionally(new Throwable("Error"));
                logger.debug("Error");
                return null;
            });
            return f;
        });
        queue.add(()-> CompletableFuture.supplyAsync(()-> {
            try {Thread.sleep(1000);} catch (InterruptedException e) {}
            logger.debug("DONE 1");
            return null;
        }));

        Thread.sleep(4000);
    }


    public void testListenableCommandCommand() throws InterruptedException {
        CommandQueue queue = new CommandQueue();
        AtomicInteger i = new AtomicInteger(0);
        ListenableCommand<String> cmd = new ListenableCommand<>(()->CompletableFuture.supplyAsync(()-> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.debug("DONE "+i.incrementAndGet());
            return "DONE "+i.get();
        }),"test");
        CompletableFuture<String> fff = queue.add(cmd::exec);
        fff.thenAccept((o)->logger.debug("here 1 " + o));
        queue.add(()->{logger.debug("Done 1.5");return CompletableFuture.completedFuture("KKo2");}).thenAccept((o)->logger.debug("here 2 "+o  ));
        queue.add(cmd::exec);
        queue.add(()->{logger.debug("Done 2.5");return CompletableFuture.completedFuture("OKOK3");}).thenAccept((o)->logger.debug("here 3"+o  ));

        Thread.sleep(3000);
    }
    public void testVoidCommand() throws InterruptedException {
        CommandQueue queue = new CommandQueue();
        AtomicInteger i = new AtomicInteger(0);
        Command<Void> cmd = ()->CompletableFuture.supplyAsync(()-> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.debug("DONE "+i.incrementAndGet());
            return null;
        });

        Command<Void> cmdWithQueue = queue.getCommand(cmd);
        CompletableFuture<Void> fff = cmdWithQueue.get();
        fff.thenAccept((o)->logger.debug("here after cmd" + o.hashCode()));
        queue.add(()->{logger.debug("Done 1.5");return CompletableFuture.completedFuture("KKo2");}).thenAccept((o)->logger.debug("here 2 "+o  ));
        queue.add(cmd);
        queue.add(()->{logger.debug("Done 2.5");return CompletableFuture.completedFuture("OKOK3");}).thenAccept((o)->logger.debug("here 3"+o  ));

        Thread.sleep(3000);
    }

}
