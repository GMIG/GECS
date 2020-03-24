package org.gmig.gecs;

import org.gmig.gecs.command.ListenableCommand;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.device.StateRequestResult;
import org.gmig.gecs.executors.TCPCommandExecutor;
import org.gmig.gecs.factories.ProjectorFactory;
import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.serialization.ObjectSerializationDecoder;
import org.apache.mina.filter.codec.serialization.ObjectSerializationEncoder;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;
import org.apache.mina.filter.codec.textline.TextLineEncoder;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.gmig.gecs.reaction.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * Created by brix isOn 2/21/2018.
 */
public class PJLinkDeviceModel {
    private TCPCommandExecutor c;

    private static final Logger logger = Logger.getLogger(PJLinkDeviceModel.class);
    private NioSocketAcceptor server = new NioSocketAcceptor();
    private InetSocketAddress testAddr;
    private static ProtocolCodecFilter textfilter = new ProtocolCodecFilter(
            new TextLineEncoder(Charset.forName("UTF-8"), LineDelimiter.MAC),new TextLineDecoder(Charset.forName("UTF-8"), LineDelimiter.MAC));
    private ProtocolCodecFilter serializationFilter = new ProtocolCodecFilter(
            new ObjectSerializationEncoder(), new ObjectSerializationDecoder());
    private static final String powerUpCommand = "%1POWR 1";
    private static final String powerDownCommand = "%1POWR 0";
    private static final String powerInfoCommand = "%1POWR ?";
    private static final String replyOk = "%1POWR=OK";
    private static final String reply1 = "%1POWR=1";
    private static final String reply0 = "%1POWR=0";
    private static final String replyErr = "%1POWR=ERR4";
    private static final String replyHello = "PJLINK 0";

    private boolean isOn = false;
    public boolean isError = false;
    public boolean isConnected = true;


    public NioSocketAcceptor setUpServer(String ip) throws IOException {
        NioSocketAcceptor server = new NioSocketAcceptor();

        server.getSessionConfig().setReuseAddress(false);
        ProtocolCodecFilter serializationFilter = textfilter;
        server.getFilterChain().addLast("codec", serializationFilter);
        server.setHandler(new IoHandlerAdapter() {
            @Override
            public void sessionOpened(IoSession session) throws Exception {
                session.write("PJLINK 0");
            }

            @Override
            public void messageReceived(IoSession session, Object message) throws Exception {
                Thread.sleep(1000);
                if (message.equals(powerUpCommand)) {
                    if (isError) {
                        session.write(replyErr);
                        return;
                    }
                    session.write(replyOk);
                    isOn = true;
                }
                if (message.equals(powerDownCommand)) {
                    session.write(replyOk);
                    isOn = false;
                }
                if (message.equals(powerInfoCommand)) {
                    if (isError) {
                        session.write(replyErr);
                        return;
                    }
                    if (isOn)
                        session.write(reply1);
                    else
                        session.write(reply0);
                }
            }

            @Override
            public void sessionClosed(IoSession session) throws Exception {
                super.sessionClosed(session);
            }
        });
        server.bind(new InetSocketAddress(InetAddress.getByName(ip),4352));//bind(new InetSocketAddress(4352));
        return server;

    }

    @Before
    public void setUp() throws Exception {
        server = setUpServer("127.0.0.1");
    }

    private void testPower(String pwr) throws ExecutionException, InterruptedException {
        ProtocolCodecFilter textfilter1 = new ProtocolCodecFilter(
                new TextLineEncoder(),
                new TextLineDecoder(){
                    @Override
                    public void decode(IoSession session, IoBuffer in, final ProtocolDecoderOutput out) throws Exception {
                        super.decode(session, in, new ProtocolDecoderOutput() {
                            public void write(Object message) {
                                //lines.add((String) message);
                                String pattern = "%1POWR=([0 1])";
                                Matcher m = Pattern.compile(pattern).matcher((String)message);
                                int res = 3;

                                if (m.find()) {
                                    res = Integer.parseInt(m.group(1));
                                }
                                else
                                    out.write(message);
                            }

                            public void flush(IoFilter.NextFilter nextFilter, IoSession session) {}
                        });

                    }
                });

        HashMap<Object, Reaction> chain = Reaction.onConnectionSuccess(new ReactionDoNothing().on(replyHello,
                new ReactionWrite(pwr).on(replyOk, new ReactionCloseWithSuccess())));
        c = new TCPCommandExecutor(textfilter, 4352);
        CompletableFuture f = c.submit("127.0.0.1",chain);
        f.get();
        assertTrue(!f.isCompletedExceptionally());
    }

    @Test
    public void testPowerOn() throws ExecutionException, InterruptedException {
        testPower(powerUpCommand);
    }

    @Test
    public void testPowerOff() throws ExecutionException, InterruptedException {
        testPower(powerDownCommand);
    }

    private CompletableFuture <Object>testPowerStatusGet() throws ExecutionException, InterruptedException {
        ReactionCloseWithSuccess closing1 = new ReactionCloseWithSuccess();
        closing1.afterCompletionAccept((o) -> logger.debug("accepting 1 " + o));
        closing1.afterCompletionAccept((o) -> logger.debug("also accepting 1 " + o));

        ReactionCloseWithSuccess closing2 = new ReactionCloseWithSuccess();
        closing2.afterCompletionAccept((o) -> logger.debug("accepting 2 " + o));
        closing2.afterCompletionAccept((o) -> logger.debug("also accepting 2 " + o));

        HashMap<Object, Reaction> chain = Reaction.onConnectionSuccess(new ReactionDoNothing().on(replyHello,
                new ReactionWrite(powerInfoCommand)
                        .on(reply1, closing1))
                        .on(reply0, closing2)
                        .on(replyErr, new ReactionCloseWithError("Projector Error")));
                //new ReactionClose());
        c = new TCPCommandExecutor( textfilter, 4352);
        return c.submit("127.0.0.1",chain);
    }

    @Test
    public void testStatusOk() throws ExecutionException, InterruptedException {
        isOn = true;
        isError = false;
        CompletableFuture<Object> f = testPowerStatusGet();
        f.get();
        Thread.sleep(1000);
    }


    @Test(expected = java.lang.IllegalStateException.class)
    public void testStatusError() throws Throwable {
        CompletableFuture f=null;
        try {
            isError = true;
            f = testPowerStatusGet();
            String r = (String) f.get();
        }
        catch (ExecutionException e){
            logger.debug(e.getCause().getMessage());
            throw e.getCause();

        }
        finally {
            assertTrue(f.isCompletedExceptionally());
        }
    }

    @Test(expected = java.lang.IllegalStateException.class)
    public void testNotConnected() throws Throwable {
        server.dispose();
        CompletableFuture f=null;
        try {
            isError = true;
            f = testPowerStatusGet();
            String r = (String) f.get();
        }
        catch (ExecutionException e){
            throw e.getCause();
        }
        finally {
            assertTrue(f.isCompletedExceptionally());
        }
    }

    private void testProjectorPowerUp(ListenableCommand<?> c) throws Exception {
        AtomicBoolean b = new AtomicBoolean(false);
        AtomicBoolean b1 = new AtomicBoolean(false);
        c.success.add((o)-> b.set(true));
        c.exception.add((o)-> b.set(false));
        CompletableFuture<?> future = c.exec().thenAccept((o)->b1.set(true));
        //Thread.sleep(1000);
        future.get();
        Thread.sleep(100);
        assertTrue(b.get());
        assertTrue(b1.get());
    }

    @Test
    public void testMultipleCommand() throws Exception {
        ManagedDevice p = ProjectorFactory.build("127.0.0.1");
        p.switchOnCmd().exec().get();
        p.switchOffCmd().exec().get();
        p.switchOnCmd().exec().get();
        p.switchOffCmd().exec().get();
        p.switchOnCmd().exec().get();
        p.switchOffCmd().exec().get();
        StateRequestResult st = p.stateReq().exec().get();
        assertFalse(st.isOn());
        p.switchOnCmd().exec().get();
        st = p.stateReq().exec().get();
        assertTrue(st.isOn());

    }

    @Test
    public void testProjectorPower() throws Exception {
        ManagedDevice p = ProjectorFactory.build("127.0.0.1");
        Boolean[] bool = new Boolean[]{true,false};
        ListenableCommand[] cmd = new ListenableCommand[]{p.switchOnCmd(),p.switchOffCmd()};
        for (Boolean t : bool) {
            for (ListenableCommand t1 : cmd) {
                isOn = t;
                testProjectorPowerUp(t1);
            }
        }
    }

    private void testCheckErrorCmd() throws ExecutionException,InterruptedException{
        ManagedDevice p = ProjectorFactory.build("127.0.0.1");
        AtomicBoolean b = new AtomicBoolean(false);
        AtomicBoolean b1 = new AtomicBoolean(false);
        p.checkCmd().exception.add((o)->b.set(true));
        CompletableFuture<?> future = p.checkCmd().exec();
        future.exceptionally((o)->{
            b1.set(true);
            return null;
        });
        future.exceptionally((o)->{
            assertTrue(b.get());
            assertTrue(b1.get());
            logger.debug("isOn " + isOn + " isError " + isError);
            return null;
        });
        future.get();
    }

    @Test
    public void testProjectorCheck() throws InterruptedException,ExecutionException {
        isError = false;
        isOn = true;
        ManagedDevice p = ProjectorFactory.build("127.0.0.1");
        AtomicBoolean b = new AtomicBoolean(false);
        AtomicBoolean b1 = new AtomicBoolean(false);
        p.checkCmd().success.add((o)->b.set(true));
        CompletableFuture<?> future = p.checkCmd().exec();
        future.thenAccept((o)->b1.set(true)).get();
        Thread.sleep(100);
        assertTrue(b.get());
        assertTrue(b1.get());
        logger.debug("isOn " + isOn + " isError " + isError);
    }

    @Test(expected = ExecutionException.class)
    public void testProjectorCheckExceptionOff() throws InterruptedException,ExecutionException {
        isError = false;
        isOn = false;
        testCheckErrorCmd();
    }
    @Test(expected = ExecutionException.class)
    public void testProjectorCheckExceptionErrorOn() throws InterruptedException,ExecutionException {
        isOn = true;
        isError = true;
        testCheckErrorCmd();
    }
    @Test(expected = ExecutionException.class)
    public void testProjectorCheckExceptionErrorOff() throws InterruptedException,ExecutionException {
        isOn = false;
        isError = true;
        testCheckErrorCmd();
    }

    private void testCheckStatusCmdOk()throws InterruptedException,ExecutionException {
        ManagedDevice p = ProjectorFactory.build("127.0.0.1");
        AtomicBoolean b = new AtomicBoolean(false);
        AtomicBoolean b1 = new AtomicBoolean(false);
        p.stateReq().success.add((o)-> {
            StateRequestResult st = o;
            logger.debug("isOn " + isOn + " isError " + isError);
            assertTrue(isOn == st.isOn());
        });
        p.stateReq().exec().get();
    }

    private void testCheckStatusCmdError()throws InterruptedException,ExecutionException {
        ManagedDevice p = ProjectorFactory.build("127.0.0.1");
        AtomicBoolean b = new AtomicBoolean(false);
        AtomicBoolean b1 = new AtomicBoolean(false);
        p.stateReq().exception.add((o)->b.set(true));
        CompletableFuture<?> future = p.stateReq().exec();
        future.exceptionally((o)->{
            b1.set(true);
            return null;
        });
        future.handle((o,t)->{
            assertTrue(b.get());
            assertTrue(b1.get());
            logger.debug("isOn " + isOn + " isError " + isError);
            return null;
        });
        future.get();
    }

    @Test
    public void testProjectorStatusOn() throws InterruptedException,ExecutionException {
        isError = false;
        isOn = true;
        testCheckStatusCmdOk();
    }

    @Test
    public void testProjectorStatusOff() throws InterruptedException,ExecutionException {
        isError = false;
        isOn = false;
        testCheckStatusCmdOk();
    }

    @Test(expected = ExecutionException.class)
    public void testProjectorStatusErrorOn() throws InterruptedException,ExecutionException {
        isOn = true;
        isError = true;
        testCheckStatusCmdError();
    }

    @Test(expected = ExecutionException.class)
    public void testProjectorStatusErrorOff() throws InterruptedException,ExecutionException {
        isOn = false;
        isError = true;
        testCheckStatusCmdError();
    }

    @Test(expected = ExecutionException.class)
    public void testProjectorDisconnected() throws InterruptedException,ExecutionException{
        ManagedDevice p = ProjectorFactory.build("127.1.0.1");
        p.switchOnCmd().exec().get();
    }


    @Test
    public void testRegularChecksOnStatus() throws InterruptedException,ExecutionException{
        ManagedDevice p = ProjectorFactory.build("127.0.0.1");
        p.manager.setDelayMillis(3000);
        int delta = p.manager.getDelayMillis();
        int numOfChecks = 3;
        AtomicInteger x = new AtomicInteger(0);
        p.checkCmd().success.add((o)->logger.debug("Increment: "+x.incrementAndGet()));
        p.switchOnCmd().exec();
        Thread.sleep(delta * numOfChecks + 500);
        logger.debug(x.get());
        assertEquals(x.get(),numOfChecks - 1);
    }

    @Test
    public void testRegularChecksOnPowerOn() throws InterruptedException,ExecutionException{
        ManagedDevice p = ProjectorFactory.build("127.0.0.1");
        p.manager.setDelayMillis(3000);
        int delta = p.manager.getDelayMillis();
        int numOfChecks = 3;
        AtomicInteger x = new AtomicInteger(0);
        p.checkCmd().success.add((o)->logger.debug("Increment: "+x.incrementAndGet()));
        p.switchOnCmd().exec();
        Thread.sleep(delta * numOfChecks + 500);
        logger.debug(x.get());
        assertEquals(numOfChecks - 1,x.get());
    }

    @Test
    public void testRegularChecksDisable() throws InterruptedException,ExecutionException{
        ManagedDevice p = ProjectorFactory.build("127.0.0.1");
        p.manager.setDelayMillis(3000);
        int delta = p.manager.getDelayMillis();
        p.manager.stopChecks();
        int numOfChecks = 2;
        AtomicInteger x = new AtomicInteger(0);
        p.checkCmd().success.add((o)->logger.debug("Increment: "+x.incrementAndGet()));
        p.switchOnCmd().exec();
        Thread.sleep(delta * numOfChecks + 500);
        assertTrue(x.get() == 1);
    }

    @Test
    public void testMassiveRegularChecks() throws InterruptedException, ExecutionException, IOException {
       setUpServer("127.0.0.2");
        setUpServer("127.0.0.3");
        setUpServer("127.0.0.4");

        ManagedDevice p = ProjectorFactory.build("127.0.0.1");
        ManagedDevice p2 = ProjectorFactory.build("127.0.0.2");
        ManagedDevice p3 = ProjectorFactory.build("127.0.0.3");
        ManagedDevice p4 = ProjectorFactory.build("127.0.0.4");

        p.manager.setDelayMillis(500);
        p2.manager.setDelayMillis(500);
        p3.manager.setDelayMillis(500);
        p4.manager.setDelayMillis(500);

        AtomicInteger x = new AtomicInteger(0);
        AtomicInteger err = new AtomicInteger(0);

        p.checkCmd().success.add((o)->logger.debug("Increment: "+x.incrementAndGet()));
        p.checkCmd().exception.add((o)->err.incrementAndGet());
        p.switchOnCmd().exec();
        p2.checkCmd().success.add((o)->logger.debug("Increment: "+x.incrementAndGet()));
        p2.checkCmd().exception.add((o)->err.incrementAndGet());
        p2.switchOnCmd().exec();

        p3.checkCmd().success.add((o)->logger.debug("Increment: "+x.incrementAndGet()));
        p3.checkCmd().exception.add((o)->err.incrementAndGet());
        p3.switchOnCmd().exec();
        p4.checkCmd().success.add((o)->logger.debug("Increment: "+x.incrementAndGet()));
        p4.checkCmd().exception.add((o)->err.incrementAndGet());
        p4.switchOnCmd().exec();


        Thread.sleep(10000);
        p.switchOffCmd().exec();
        p.queue.clear();
        p2.switchOffCmd().exec();
        p2.queue.clear();

        p3.switchOffCmd().exec();
        p3.queue.clear();

        p4.switchOffCmd().exec();
        p4.queue.clear();

        Thread.sleep(1000);

        assertTrue(err.get()==0);

    }

    @Test
    public void testMassiveRegularChecks1() throws InterruptedException, ExecutionException, IOException {
        ManagedDevice p = ProjectorFactory.build("127.0.0.1");

        p.manager.setDelayMillis(1000);

        AtomicInteger x = new AtomicInteger(0);
        AtomicInteger err = new AtomicInteger(0);

        p.checkCmd().success.add((o)->logger.debug("Increment: "+x.incrementAndGet()));
        p.checkCmd().exception.add((o)->err.incrementAndGet());
        p.switchOnCmd().exec();
        Thread.sleep(10000);
        p.switchOffCmd().exec();
        Thread.sleep(10000);

        assertTrue(err.get()==0);

    }

    @After
    public void tearDown() {
        if (c!=null)
            c.dispose();
        if (server!=null)
            server.dispose();

    }
}
