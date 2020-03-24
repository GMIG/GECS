package org.gmig.gecs;

import org.gmig.gecs.command.Command;
import org.gmig.gecs.command.ComplexCommandBuilder;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by brix on 4/4/2018.
 */
@SuppressWarnings("EmptyCatchBlock")
public class ComplexCommandBuilderTest {
    private static final Logger logger = Logger.getLogger(ComplexCommandBuilderTest.class);

    private final ArrayList<Supplier<CompletableFuture<String>>> cmds = new ArrayList<>();

    Supplier<CompletableFuture<String>> fu(Supplier<CompletableFuture<String>> old,int i, ArrayList<String> result){
        Supplier<CompletableFuture<String>>add =  cmds.get(i);
        return ()->{
            return old.get().thenCompose((r)->{
                result.add(r);
                logger.debug(result);
                return add.get();});
        };
    }
    @Test
    public void testBuildt() throws Exception {
       /*  Supplier<CompletableFuture<String>> cmd1 = ()-> CompletableFuture.supplyAsync(()->{
            try {Thread.sleep(1000);} catch (InterruptedException e) {}
            logger.debug("11");
            return "r11";
        });
        Supplier<CompletableFuture<String>> cmd2 = ()-> CompletableFuture.supplyAsync(()->{
            try {Thread.sleep(1000);} catch (InterruptedException e) {}
            logger.debug("22");
            return "r22";
        });
        Supplier<CompletableFuture<String>> cmd3 = ()-> CompletableFuture.supplyAsync(()->{
            try {Thread.sleep(1000);} catch (InterruptedException e) {}
            logger.debug("33");
            return "r33";
        });
        Supplier<CompletableFuture<String>> cmde = ()-> {
            CompletableFuture<String> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> {
                try { Thread.sleep(2000); } catch (InterruptedException e) { }
                future.completeExceptionally(new Throwable("error"));
            });
            return future;
        };
        Supplier<CompletableFuture<String>> cmd4 = ()-> CompletableFuture.supplyAsync(()->{
            try {Thread.sleep(1000);} catch (InterruptedException e) {}
            logger.debug("44");
            return "r44";
        });
        cmds = new ArrayList<>(Arrays.asList(cmde,cmd1));
        ArrayList<String> total = new ArrayList<>();
        Supplier<CompletableFuture<String>> cmdnew = cmds.get(0);

        for(AtomicInteger i = new AtomicInteger(1); i.get()<cmds.size(); i.incrementAndGet()){
            cmdnew = fu(cmdnew,i.get(),total);
        }
        CompletableFuture <String> result = new CompletableFuture<>();
        CompletableFuture <Void> fut = cmdnew.get().handle((o,t)->{
            if (t!=null) {
                logger.debug(t);
                result.completeExceptionally(t);
            }
            else {
                total.add(o);
                result.complete(o);
            }
            return null;
        });
        result.get();
        logger.debug(result);

        logger.debug(total);


       Supplier<CompletableFuture<String>> cmd12 = ()->{
            return cmd1.get().thenCompose((r)->{total.add(r);logger.debug(total);return cmd2.get();});
        };
        Supplier<CompletableFuture<String>> cmd123 = ()->{
            return cmd12.get().thenCompose((r)->{total.add(r);logger.debug(total);return cmd3.get();});
        };
        Supplier<CompletableFuture<String>> cmd1234 = ()->{
            return cmd123.get().thenCompose((r)->{total.add(r);logger.debug(total);return cmd4.get();});
        };
        cmd1234.get().thenAccept((r)->{total.add(r);logger.debug(total);}).get();
        Thread.sleep(1000);*/

    }
    @Test(expected = ExecutionException.class)
    public void testBuildSequenceRError() throws Exception {

        AtomicBoolean b2 = new AtomicBoolean(false);
        AtomicBoolean err = new AtomicBoolean(false);
        AtomicBoolean b3 = new AtomicBoolean(false);

        Command<?> cmd =  ComplexCommandBuilder.builder().
                addCommand(0,"run11",() -> {
                    CompletableFuture<Object> future = new CompletableFuture<>();
                    CompletableFuture.runAsync(() -> {
                        try {Thread.sleep(1000);} catch (InterruptedException e) {}
                        future.completeExceptionally(new Throwable("error"));
                    });
                    return future;
                }).
                addCommand(1,"run22",()->
                        CompletableFuture.supplyAsync(()->{
                            try {Thread.sleep(2000);} catch (InterruptedException e) {}
                            logger.debug("22");
                            b3.set(true);
                            return "22";
                        }))

                .build();
        CompletableFuture<?> f = cmd.get();
        logger.debug(f);
        f.exceptionally((t)->{err.set(true);return null;});
        Thread.sleep(100);
        //assertFalse(b2.get() && err.get() );
        logger.debug(b2.get() +" " + err.get() +" "+ b3.get());

        Thread.sleep(2000);
        logger.debug(b2.get() +" " + err.get() +" "+ b3.get());
        //assertTrue(b2.get() && !err.get());
        Thread.sleep(2000);
        logger.debug(b2.get() +" " + err.get() +" "+ b3.get());
        Thread.sleep(2000);
        logger.debug(b2.get() +" " + err.get() +" "+ b3.get());

        // assertTrue(b2.get() && err.get() );
        logger.debug(f);
        HashMap g = (HashMap) f.get();
        logger.debug(g);
        Thread.sleep(3000);
    }
    @Test
    public void testBuildSequence() throws Exception {

        AtomicBoolean b1 = new AtomicBoolean(false);
        AtomicBoolean b2 = new AtomicBoolean(false);
        AtomicBoolean b3 = new AtomicBoolean(false);

        Command<?> cmd =  ComplexCommandBuilder.builder().
                addCommand(0,"run11",() ->
                        CompletableFuture.supplyAsync(()->{
                            try {Thread.sleep(3000);} catch (InterruptedException e) {}
                            logger.debug("11");
                            b1.set(true);
                            return "11";
                        })).
                addCommand(1,"run21",()->
                        CompletableFuture.supplyAsync(()->{
                            try {Thread.sleep(2000);} catch (InterruptedException e) {}
                            logger.debug("21");
                            b2.set(true);
                            return "21";
                        })).
                addCommand(2,"run22",()->
                        CompletableFuture.supplyAsync(()->{
                            try {Thread.sleep(1000);} catch (InterruptedException e) {}
                            logger.debug("22");
                            b3.set(true);
                            return "22";
                        }))
                .build();
        CompletableFuture f = cmd.get();
        Thread.sleep(100);
        assertFalse(b1.get() && b2.get() && b3.get());
        Thread.sleep(3000);
        assertTrue(b1.get() && !b2.get() && !b3.get());
        Thread.sleep(2000);
        assertTrue(b1.get() && b2.get() && !b3.get());
        Thread.sleep(1000);
        assertTrue(b1.get() && b2.get() && b3.get());
        logger.debug(f);
        HashMap g = (HashMap) f.get();
        logger.debug(g);
    }
    @Test
    public void testBuildSequence001() throws Exception {

        AtomicBoolean b1 = new AtomicBoolean(false);
        AtomicBoolean b2 = new AtomicBoolean(false);
        AtomicBoolean b3 = new AtomicBoolean(false);

        Command<?> cmd =  ComplexCommandBuilder.builder().
                addCommand(0,"run11",() ->
                        CompletableFuture.supplyAsync(()->{
                            try {Thread.sleep(3000);} catch (InterruptedException e) {}
                            logger.debug("11");
                            b1.set(true);
                            return "11";
                        })).
                addCommand(0,"run21",()->
                        CompletableFuture.supplyAsync(()->{
                            try {Thread.sleep(2000);} catch (InterruptedException e) {}
                            logger.debug("21");
                            b2.set(true);
                            return "21";
                        })).
                addCommand(1,"run22",()->
                        CompletableFuture.supplyAsync(()->{
                            try {Thread.sleep(1000);} catch (InterruptedException e) {}
                            logger.debug("22");
                            b3.set(true);
                            return "22";
                        }))
                .build();
        CompletableFuture f = cmd.get();
        Thread.sleep(100);
        assertFalse(b1.get() && b2.get() && b3.get());
        Thread.sleep(3000);
        assertTrue(b1.get() && b2.get() && !b3.get());
        Thread.sleep(1000);
        assertTrue(b1.get() && b2.get() && b3.get());
        Thread.sleep(2000);
        logger.debug(f);
        HashMap g = (HashMap) f.get();
        logger.debug(g);
    }


    @Test
    public void testBuildFork() throws Exception {

        AtomicBoolean b1 = new AtomicBoolean(false);
        AtomicBoolean b21 = new AtomicBoolean(false);
        AtomicBoolean b22 = new AtomicBoolean(false);
        AtomicBoolean b3 = new AtomicBoolean(false);

        Command<?> cmd =  ComplexCommandBuilder.builder().
                addCommand(0,"run11",() ->
                        CompletableFuture.supplyAsync(()->{
                            try {Thread.sleep(3000);} catch (InterruptedException e) {}
                            logger.debug("11");
                            b1.set(true);
                            return "11";
                        })).
                addCommand(1,"run21",()->
                        CompletableFuture.supplyAsync(()->{
                            try {Thread.sleep(1000);} catch (InterruptedException e) {}
                            logger.debug("21");
                            b21.set(true);
                            return "21";
                        })).
                addCommand(1,"run22",()->
                        CompletableFuture.supplyAsync(()->{
                            try {Thread.sleep(2000);} catch (InterruptedException e) {}
                            logger.debug("22");
                            b22.set(true);
                            return "22";
                        })).
                addCommand(2,"run31",()->
                        CompletableFuture.supplyAsync(()->{
                            try {Thread.sleep(1000);} catch (InterruptedException e) {}
                            logger.debug("31");
                            b3.set(true);

                            return "31";
                        }))
                .build();
        CompletableFuture f = cmd.get();
        Thread.sleep(100);
        assertFalse(b1.get() && b21.get() && b22.get() && b3.get());
        Thread.sleep(3000);
        assertTrue(b1.get() && !b21.get() && !b22.get() && !b3.get());
        Thread.sleep(1000);
        assertTrue(b1.get() && b21.get() && !b22.get() && !b3.get());
        Thread.sleep(1000);
        assertTrue(b1.get() && b21.get() && b22.get() && !b3.get());
        Thread.sleep(1000);
        assertTrue(b1.get() && b21.get() && b22.get() && b3.get());
        logger.debug(f);
        HashMap g = (HashMap) f.get();
        logger.debug(g);
    }
    @Test
    public void testCollectFirstLaterThenSecond() throws Exception {

        AtomicBoolean b1 = new AtomicBoolean(false);
        AtomicBoolean b2 = new AtomicBoolean(false);

        Command<HashMap<String, ?>> cmd = ComplexCommandBuilder.builder()
                .addCommand(0, "run11", () ->
                        CompletableFuture.supplyAsync(() -> {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                            }
                            b1.set(true);
                            logger.debug("11");
                            return "11";
                        }))
                .addCommand(0, "run12", () ->
                        CompletableFuture.supplyAsync(() -> {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                            }
                            b2.set(true);
                            logger.debug("12");
                            return "12";
                        })).collect(0);
        CompletableFuture<HashMap<String, ?>> f = cmd.get();
        Thread.sleep(100);
        assertFalse(b2.get() && b1.get());
        Thread.sleep(1000);
        assertTrue(b2.get() && !b1.get());
        Thread.sleep(1100);
        assertTrue(b1.get() && b2.get());
        HashMap g = f.get();
        logger.debug(g);
        assertTrue(g.containsKey("run11"));
        assertTrue(g.containsValue("11"));
        assertTrue(g.containsKey("run12"));
        assertTrue(g.containsValue("12"));

        f.get();

    }
    @Test
    public void testCollectFirstEarlierThenSecond() throws Exception {

        AtomicBoolean b1 = new AtomicBoolean(false);
        AtomicBoolean b2 = new AtomicBoolean(false);

        Command<HashMap<String,?>> cmd =  ComplexCommandBuilder.builder()
                .addCommand(0,"run11",() ->
                        CompletableFuture.supplyAsync(()->{
                            try {Thread.sleep(1000);} catch (InterruptedException e) {}
                            b1.set(true);
                            logger.debug("11");
                            return "11";
                        }))
                .addCommand(0,"run12",()->
                        CompletableFuture.supplyAsync(()->{
                            try {Thread.sleep(2000);} catch (InterruptedException e) {}
                            b2.set(true);
                            logger.debug("12");
                            return "12";
                        })).collect(0);
        CompletableFuture<HashMap<String,?>> f = cmd.get();
        Thread.sleep(100);
        assertFalse(b1.get() && b2.get());
        Thread.sleep(1000);
        assertTrue(b1.get() && !b2.get());
        Thread.sleep(1100);
        assertTrue(b1.get() && b2.get());
        HashMap g = f.get();
        logger.debug(g);
        assertTrue(g.containsKey("run11"));
        assertTrue(g.containsValue("11"));
        assertTrue(g.containsKey("run12"));
        assertTrue(g.containsValue("12"));
    }

    @Test (expected = ExecutionException.class)
    public void testCollectException() throws InterruptedException,ExecutionException {
        CompletableFuture<HashMap<String, ?>> f = null;
    try {
        AtomicBoolean b1 = new AtomicBoolean(false);
        AtomicBoolean err = new AtomicBoolean(false);
        AtomicBoolean err1 = new AtomicBoolean(false);

        Command<HashMap<String, ?>> cmd = ComplexCommandBuilder.builder()
                .addCommand(0, "run11", () ->
                        CompletableFuture.supplyAsync(() -> {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                            }
                            b1.set(true);
                            logger.debug("11");
                            return "11";
                        }))
                .addCommand(0, "run12", () -> {
                    CompletableFuture<Object> future = new CompletableFuture<>();
                    CompletableFuture.runAsync(() -> {
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                        }
                        future.completeExceptionally(new Throwable("error"));
                    });
                    return future;
                }).collect(0);
         f = cmd.get();
        f.exceptionally((t) -> {
            err.set(true);
            return null;
        });
        assertFalse(b1.get() && err.get());
        Thread.sleep(1100);
        assertTrue(b1.get() && !err.get());
        Thread.sleep(1100);
        logger.debug(b1.get() + " " + err.get());
        assertTrue(b1.get() && err.get());
        f.get();
    }
    finally {
        logger.debug(f);

    }

    }

    @Test (expected = ExecutionException.class)
    public void testCollectException2() throws ExecutionException,InterruptedException {

        CompletableFuture<HashMap<String,?>> f = null;
        try {
            AtomicBoolean b1 = new AtomicBoolean(false);
            AtomicBoolean err = new AtomicBoolean(false);
            AtomicBoolean b3 = new AtomicBoolean(false);

            Command<HashMap<String, ?>> cmd = ComplexCommandBuilder.builder()
                    .addCommand(0, "run11", () ->
                            CompletableFuture.supplyAsync(() -> {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                }
                                b1.set(true);
                                logger.debug("11");
                                return "11";
                            }))
                    .addCommand(0, "run12", () -> {
                        CompletableFuture<Object> future = new CompletableFuture<>();
                        CompletableFuture.runAsync(() -> {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                            }
                            future.completeExceptionally(new Throwable("error"));
                        });
                        return future;
                    })
                    .addCommand(0, "run13", () ->
                            CompletableFuture.supplyAsync(() -> {
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException e) {
                                }
                                b3.set(true);
                                logger.debug("13");
                                return "13";
                            })).collect(0);
            f = cmd.get();
            f.exceptionally((t) -> {
                err.set(true);
                return null;
            });
            assertFalse(b1.get() && err.get() && !b3.get());
            Thread.sleep(1100);
            logger.debug(b1.get() + " " + err.get());
            assertTrue(b1.get() && !err.get() && !b3.get());
            Thread.sleep(1100);
            logger.debug(b1.get() + " " + err.get());
            assertTrue(b1.get() && !err.get() && !b3.get());
            Thread.sleep(1100);
            logger.debug(b1.get() + " " + err.get());
            assertTrue(b1.get() && err.get() && b3.get());
            logger.debug(f.get());
        }
        finally {
            logger.debug(f);
            //assertTrue(f.isCompletedExceptionally());
        }


    }
    @Test (expected = ExecutionException.class)
    public void testCollectException3() throws ExecutionException,InterruptedException {

        CompletableFuture<HashMap<String,?>> f = null;
        try {
            AtomicBoolean err = new AtomicBoolean(false);

            Command<HashMap<String, ?>> cmd = ComplexCommandBuilder.builder()
                    .addCommand(0, "run12", () -> {
                        CompletableFuture<Object> future = new CompletableFuture<>();
                        CompletableFuture.runAsync(() -> {
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                            }
                            future.completeExceptionally(new Throwable("error"));
                        });
                        return future;
                    }).collect(0);
            f = cmd.get();
            f.exceptionally((t) -> {
                err.set(true);
                return null;
            });
            logger.debug(f);
            f.get();
            logger.debug(f);
        }
        finally {
            logger.debug(f);
            //assertTrue(f.isCompletedExceptionally());
        }


    }


}