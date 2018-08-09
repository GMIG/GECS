package org.gmig.gecs;

import org.gmig.gecs.executors.TCPCommandExecutor;
import org.gmig.gecs.executors.TCPReactionHandler;
import org.apache.log4j.Logger;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationDecoder;
import org.apache.mina.filter.codec.serialization.ObjectSerializationEncoder;
import org.apache.mina.filter.codec.textline.TextLineDecoder;
import org.apache.mina.filter.codec.textline.TextLineEncoder;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.gmig.gecs.reaction.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

public class TCPCommandExecutorTest {
    TCPCommandExecutor c;

    private static final Logger logger = Logger.getLogger(TCPCommandExecutorTest.class);
    private NioSocketAcceptor server = new NioSocketAcceptor();
    private NioSocketAcceptor server2;
    private InetSocketAddress testAddr;
    private ProtocolCodecFilter textfilter = new ProtocolCodecFilter(
                    new TextLineEncoder(),
                    new TextLineDecoder());
    private ProtocolCodecFilter serializationFilter = new ProtocolCodecFilter(
            new ObjectSerializationEncoder(), new ObjectSerializationDecoder());


    @Before
    public void setUp() throws Exception {
        try {
            testAddr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 11212);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        server.getSessionConfig().setReuseAddress(false);
        //server.getSessionConfig().setWriteTimeout(30);
        ProtocolCodecFilter serializationFilter = new ProtocolCodecFilter(new TextLineEncoder(),
                new TextLineDecoder());
        server.getFilterChain().addLast("codec", serializationFilter);
        server.setHandler(new IoHandlerAdapter(){
            @Override
            public void sessionOpened(IoSession session) throws Exception {
                logger.debug("DebugServer:sessionOpened ");
            }

            @Override
            public void sessionClosed(IoSession session) throws Exception {
                logger.debug("DebugServer:sessionClosed ");
            }

            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                logger.debug("DebugServer:received " + message);
            }
        });


    }

    @Test
    public void testExpectedException() throws InterruptedException, ExecutionException, NoSuchFieldException, IllegalAccessException, IOException {
            logger.debug("Binding");
            server.setHandler(new IoHandlerAdapter(){
                @Override
                public void messageReceived(IoSession session, Object message) throws Exception {
                   // session.closeNow();
                }
            });
            server.bind(new InetSocketAddress(11212));
            //future3.getSession().getHandler().exceptionCaught( future3.getSession(), new IOException("fdfd"));

            HashMap<Object,Reaction> reactionMap = Reaction.onConnectionSuccess(new ReactionWrite("hio").on("class java.io.IOException",new ReactionCloseWithSuccess()));
            TCPCommandExecutor ooooas = new TCPCommandExecutor(textfilter,11212);
            Field field = null;
            field = TCPCommandExecutor.class.getDeclaredField("connector");
            field.setAccessible(true);
            field.get(ooooas);
            ((TCPReactionHandler)((NioSocketConnector)field.get(ooooas)).getHandler()).setException = true;

            CompletableFuture<Object> future =  ooooas.submit("127.0.0.1",reactionMap);
            future.get();
            assertTrue(!future.isCompletedExceptionally());

    }

    @Test (expected = IllegalStateException.class)
    public void testUnexpectedException() throws  java.lang.Throwable {
        CompletableFuture<Object> future = null;
        try {
            logger.debug("Binding");
            server.setHandler(new IoHandlerAdapter(){
                @Override
                public void messageReceived(IoSession session, Object message) throws Exception {
                    // session.closeNow();
                }
            });
            server.bind(new InetSocketAddress(11212));
            //future3.getSession().getHandler().exceptionCaught( future3.getSession(), new IOException("fdfd"));

            HashMap<Object,Reaction> reactionMap = Reaction.onConnectionSuccess(new ReactionWrite("hio"));
            TCPCommandExecutor ooooas = new TCPCommandExecutor(textfilter,11212);
            Field field = null;
            field = TCPCommandExecutor.class.getDeclaredField("connector");
            field.setAccessible(true);
            field.get(ooooas);
            ((TCPReactionHandler)((NioSocketConnector)field.get(ooooas)).getHandler()).setException = true;

            future =  ooooas.submit("127.0.0.1",reactionMap);
            future.get();
        } catch (ExecutionException e){
            //e.printStackTrace();
            throw e.getCause();
        }
        finally {
            assertTrue(future.isCompletedExceptionally());
        }
    }


    @Test
    public void testCloseUnit() throws InterruptedException, IOException, ExecutionException {
            logger.debug("Binding");
            server.setHandler(new IoHandlerAdapter(){
                @Override
                public void messageReceived(IoSession session, Object message) throws Exception {
                    Thread.sleep(1000);
                    session.closeNow();
                }
            });
            server.bind(new InetSocketAddress(11212));
            HashMap<Object,Reaction> reactionMap = Reaction.onConnectionSuccess(new ReactionWrite("ff")
                                                                            .thenAwaitClosing(new ReactionCloseWithSuccess()));
            c = new TCPCommandExecutor(textfilter,11212);
            CompletableFuture future =  c.submit("127.0.0.1",reactionMap);
            future.get();
            assertTrue(!future.isCompletedExceptionally());

    }

    public CompletableFuture testComplexQueue(String resp) throws IOException {
        logger.debug("Binding");
        server.setHandler(new IoHandlerAdapter(){
            @Override
            public void sessionCreated(IoSession session) throws Exception {
                session.write("hello");
            }
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                if ( message.equals("wanna talk?"))
                    session.write("yep");
                else
                    session.write(resp);
            }
        });
        server.bind(new InetSocketAddress(11212));
        HashMap<Object,Reaction> chain = Reaction.onConnectionSuccess(new ReactionDoNothing().on("hello",
                new ReactionWrite("wanna talk?").on("yep",
                        new ReactionWrite("how are you?").on("fine",new ReactionCloseWithSuccess())
                                .on("not good",new ReactionCloseWithError("He is not good"))
                                .on("so-so",new ReactionCloseWithSuccess()))));
        c = new TCPCommandExecutor(textfilter,11212);
        c.setReadTimeoutSeconds(100);

        return c.submit("127.0.0.1",chain);
    }
    HashMap<Object,Reaction> chain;
    @Test
    public void testMultiple() throws Exception {
        logger.debug("Binding");
        server.setHandler(new IoHandlerAdapter(){
            @Override
            public void sessionCreated(IoSession session) throws Exception {
                session.write("hello");
            }
            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                if ( message.equals("wanna talk?"))
                    session.write("yep");
                else
                    session.write("fine");
            }
        });
        server.bind(new InetSocketAddress(11212));
        chain = Reaction.onConnectionSuccess(new ReactionDoNothing().on("hello",
                new ReactionWrite("wanna talk?").on("yep",
                        new ReactionWrite("how are you?").on("fine",new ReactionCloseWithSuccess())
                                .on("not good",new ReactionCloseWithError("He is not good"))
                                .on("so-so",new ReactionCloseWithSuccess()))));
        c = new TCPCommandExecutor(textfilter,11212);
        c.setReadTimeoutSeconds(1000);

        for (int i = 0; i < 100; i++) {
            c.submit("127.0.0.1",chain);
            Thread.sleep(300);
        }

    }

    @Test
    public void testComplexQueueAllOk() throws ExecutionException, InterruptedException, IOException {
        CompletableFuture o = testComplexQueue("fine");
            o.get();
            assertTrue(!o.isCompletedExceptionally());
    }

    @Test(expected = ExecutionException.class)
    public void testComplexQueueAllBad() throws ExecutionException, InterruptedException, IOException {
        CompletableFuture o = null;
        try {
            o = testComplexQueue("not good");
            o.get();
        }
        finally {
            assertTrue(o.isCompletedExceptionally());
        }
    }

    @Test(expected = ExecutionException.class)
    public void testUnexpectedCloseUnit() throws ExecutionException, InterruptedException, IOException {
        CompletableFuture future = null;
    try {
        logger.debug("Binding");
        server.setHandler(new IoHandlerAdapter() {
            @Override
            public void sessionOpened(IoSession session) throws Exception {
                Thread.sleep(1000);
                session.closeNow();
            }
        });
        server.bind(new InetSocketAddress(11212));
        HashMap<Object, Reaction> reactionMap = Reaction.onConnectionSuccess(new ReactionDoNothing());
        c = new TCPCommandExecutor( textfilter, 11212);
        future = c.submit("127.0.0.1",reactionMap);
        future.get();
    }
    finally {
        assertTrue(future.isCompletedExceptionally());
    }

    }

    @Test
    public void testCloseCS() throws InterruptedException, ExecutionException, IOException {
            logger.debug("Binding");
            server.setHandler(new IoHandlerAdapter(){
                @Override
                public void messageReceived(IoSession session, Object message) throws Exception {
                    Thread.sleep(1000);
                    session.write("hi");
                }
            });
            server.bind(new InetSocketAddress(11212));
            HashMap<Object,Reaction> reactionMap = Reaction.onConnectionSuccess(new ReactionWrite("tt").on("hi",new ReactionCloseWithSuccess()));
            c = new TCPCommandExecutor( textfilter,11212);
            CompletableFuture<Object> future = c.submit("127.0.0.1",reactionMap);
            future.get();
            assertTrue(!future.isCompletedExceptionally());

            //future.exec();


    }

    @Test
    public void testNonConnection() throws ExecutionException, InterruptedException {
            HashMap<Object,Reaction> reactionMap = Reaction.onConnectionSuccess(new ReactionCloseWithSuccess());
            c = new TCPCommandExecutor( textfilter,11212);
            CompletableFuture<Object> future = c.submit("127.0.0.1",reactionMap);

            logger.debug("next");
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.schedule(()->{
                        try {
                            server.bind(new InetSocketAddress(11212));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
            , 5, TimeUnit.SECONDS);
            future.get();
            assertTrue(!future.isCompletedExceptionally());

    }

    @Test(expected = ExecutionException.class)
    public void testNonConnectionEver() throws UnknownHostException, ExecutionException, InterruptedException {
        CompletableFuture<Object> future=null;
        try {
            HashMap<Object, Reaction> reactionMap = Reaction.onConnectionSuccess(new ReactionDoNothing());
            c = new TCPCommandExecutor( textfilter,11212);
            c.setMaxReconnectTries(2);
            c.setReconnectTimeMillis(1000);
            future = c.submit("127.0.0.1",reactionMap);
            future.get();
        }
        finally {
            assertTrue(future.isCompletedExceptionally());

        }
    }

    @Test
    public void testExpectedNonConnectionEver() throws UnknownHostException, ExecutionException, InterruptedException {
            HashMap<Object, Reaction> reactionMap = Reaction.onConnectionFail(new ReactionCloseWithSuccess());
            c = new TCPCommandExecutor( textfilter,11212);
            c.setMaxReconnectTries(2);
            c.setReconnectTimeMillis(1000);
            CompletableFuture<Object> future = c.submit("127.0.0.1",reactionMap);
            future.get();
            assertTrue(!future.isCompletedExceptionally());
        }

    @Test (expected = ExecutionException.class)
    public void testUnexpectedIdle() throws IOException, ExecutionException, InterruptedException {
        server.bind(new InetSocketAddress(11212));
        CompletableFuture<Object> future = null;
        try {
            HashMap<Object, Reaction> reactionMap = Reaction.onConnectionSuccess(new ReactionDoNothing());
            c = new TCPCommandExecutor( textfilter,11212);
            future = c.submit("127.0.0.1",reactionMap);
            future.get();
        }
        finally {
            assertTrue(future.isCompletedExceptionally());
        }
    }


    @Test(expected = IllegalStateException.class)
    public void testUnexpectedMessage() throws ExecutionException, InterruptedException, IOException {
        CompletableFuture<Object> future = null;
        try {
            logger.debug("Binding");
            server.getSessionConfig().setWriteTimeout(30);

            server.setHandler(new IoHandlerAdapter(){
                @Override
                public void sessionOpened(IoSession session) throws Exception {
                    session.write("Unknown Message");
                }
            });
            server.bind(new InetSocketAddress(11212));
            HashMap<Object,Reaction> reactionMap = Reaction.onConnectionSuccess(new ReactionWrite("oo"));
            c = new TCPCommandExecutor( textfilter,11212);
            future = c.submit("127.0.0.1",reactionMap);
            future.get();

        } catch (ExecutionException e){
            logger.debug(e.getCause());
            throw (IllegalStateException) e.getCause();
        }
        finally {
            assertTrue(future.isCompletedExceptionally());
        }
    }

    class Lamp{
        String myName;

        void turnOn(){
            logger.debug("Light is isOn");
        }
    }



    @Test
    public void testThenAccept() throws ExecutionException, InterruptedException, IOException {
        CompletableFuture future = null;
        try {
            logger.debug("Binding");
            server.setHandler(new IoHandlerAdapter(){
                @Override
                public void sessionOpened(IoSession session) throws Exception {
                    session.write("oo");
                }
            });
            server2 = new NioSocketAcceptor();
            server2.setHandler(new IoHandlerAdapter(){
                @Override
                public void sessionOpened(IoSession session) throws Exception {
                    session.write("ok");
                }
            });
            server.getFilterChain().addLast("ser",textfilter);
            server2.getFilterChain().addLast("ser",textfilter);

            server.bind(new InetSocketAddress("127.0.0.1",11212));
            server2.bind(new InetSocketAddress("127.0.0.2",11212));

            ReactionCloseWithSuccess r = new ReactionCloseWithSuccess();
            r.afterCompletionAccept((msg)-> {logger.debug(msg);});
            r.afterCompletionAccept((msg)-> {logger.debug(" dixi ");});

            HashMap<Object,Reaction> reactionMap = Reaction.onConnectionSuccess(r);
            c = new TCPCommandExecutor(textfilter,11212);
            CompletableFuture future1 = c.submit("127.0.0.1",reactionMap);
            CompletableFuture future2 = c.submit("127.0.0.2",reactionMap);
            CompletableFuture f = CompletableFuture.allOf(future1,future2);
            f.get();

            Thread.sleep(1000);
        } catch (ExecutionException e){
            logger.debug(e.getCause());
            throw (IllegalStateException) e.getCause();
        }
        finally {
        }

    }



    @Test(expected = ExecutionException.class)
    public void testObjects() throws ExecutionException, IOException, InterruptedException {
        CompletableFuture future = null;
        CompletableFuture future2 = null;
        CompletableFuture future3 = null;

        try {
            logger.debug("Binding");
            server.dispose();
            server = new NioSocketAcceptor();
            server2 = new NioSocketAcceptor();
            server.getFilterChain().addLast("ser",serializationFilter);
            server.getFilterChain().addLast("log",new LoggingFilter());
            server2.getFilterChain().addLast("ser",serializationFilter);
            server2.getFilterChain().addLast("log",new LoggingFilter());

            server.setHandler(new IoHandlerAdapter(){
                @Override
                public void sessionOpened(IoSession session) throws Exception {
                    session.write(new Somobject(1,"popo"));
                }
            });
            server2.setHandler(new IoHandlerAdapter(){
                @Override
                public void sessionOpened(IoSession session) throws Exception {
                    session.write(new Somobject(1,"popa"));
                }
            });
            server.bind(new InetSocketAddress("127.0.0.1",11212));
            server2.bind(new InetSocketAddress("127.0.0.2",11212));

            HashMap<Object,Reaction> reactionMap = Reaction.onConnectionSuccess(new ReactionDoNothing().
                    on(new Somobject(1,"popo"), new ReactionCloseWithSuccess()));

            TCPCommandExecutor c = new TCPCommandExecutor(serializationFilter,11212);

             future = c.submit(("127.0.0.1"),reactionMap);
             future2 = c.submit(("127.0.0.2"),reactionMap);

            CompletableFuture<Void> combinedFuture  = CompletableFuture.allOf(future, future2);
            combinedFuture.get();

        }
        catch(ExecutionException e){
            throw e;
        }finally {
            assertTrue(future2.isCompletedExceptionally());
                assertTrue(future.get() instanceof Somobject);
                assertTrue(((Somobject)future.get()).b.equals("popo"));
                assertTrue(((Somobject)future.get()).a == 1);
        }

    }

    @After
    public void tearDown() throws Exception {
        if (c!=null)
            c.dispose();
        if (server!=null)
            server.dispose();
        if (server2!=null)
            server2.dispose();

    }

}