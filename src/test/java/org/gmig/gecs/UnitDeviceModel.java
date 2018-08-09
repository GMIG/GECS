package org.gmig.gecs;

import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.factories.VLCPlayer2ScreenFactory;
import org.gmig.gecs.factories.VLCPlayerFactory;
import org.gmig.gecs.factories.VLCTestPlayerFactory;
import org.apache.log4j.Logger;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.serialization.ObjectSerializationDecoder;
import org.apache.mina.filter.codec.serialization.ObjectSerializationEncoder;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;
import org.apache.mina.filter.codec.textline.TextLineEncoder;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * Created by brix isOn 3/16/2018.
 */
public class UnitDeviceModel {

    private static final Logger logger = Logger.getLogger(UnitDeviceModel.class);
    private InetSocketAddress testAddr;
    private static ProtocolCodecFilter textfilter = new ProtocolCodecFilter(
            new TextLineEncoder(Charset.forName("UTF-8"), LineDelimiter.MAC),new TextLineDecoder(Charset.forName("UTF-8"), LineDelimiter.MAC));
    private ProtocolCodecFilter serializationFilter = new ProtocolCodecFilter(
            new ObjectSerializationEncoder(), new ObjectSerializationDecoder());

    public boolean isOn = true;
    class Handler extends IoHandlerAdapter{
        public boolean isError = false;

        @Override
        public void sessionOpened(IoSession session) throws Exception {
            logger.debug("DebugServer:sessionOpened ");
            session.write("CONNECTED");
        }

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            Thread.sleep(1000);
            if (message.equals("STATUS")) {
                if(!isError)
                    session.write("1");
                else
                    session.write("2");

            }
            if (message.equals("ERROR")) {
                if(!isError)
                    session.write("1");
                else
                    session.write("5");

            }
            if (message.equals("SHUTDOWN")) {
                session.closeNow();
            }
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            super.sessionClosed(session);
        }
    }
    class HandlerDaemon extends IoHandlerAdapter{

        @Override
        public void messageReceived(IoSession session, Object message) throws Exception {
            if (message.equals("SHUTDOWN")) {
                isOn = false;
                session.closeNow();
            }
        }

        @Override
        public void sessionClosed(IoSession session) throws Exception {
            super.sessionClosed(session);
        }
    }

    public NioSocketAcceptor setUpServer(String ip,int port) throws IOException {
        NioSocketAcceptor server = new NioSocketAcceptor();

        //server.getSessionConfig().setReuseAddress(false);
        server.getFilterChain().addLast("codec", textfilter);
        server.setHandler(new Handler());

        if (isOn) {
            server.bind(new InetSocketAddress(InetAddress.getByName(ip), port));
        }
        return server;
    }

    public NioSocketAcceptor setUpDaemon(String ip) throws IOException {
        NioSocketAcceptor serverDaemon = new NioSocketAcceptor();

        serverDaemon.getFilterChain().addLast("codec", textfilter);
        serverDaemon.setHandler(new HandlerDaemon());

        if (isOn) {
            serverDaemon.bind(new InetSocketAddress(InetAddress.getByName(ip), 10203));
        }
        return serverDaemon;
    }

    @Before
    public void setUp() throws Exception {
        //server = setUpServer("127.0.0.1");
    }

    @Test @Ignore
     public void player() throws Exception {
       // ((Handler)setUpServer("127.0.0.1",11211).getHandler()).isError = false;
        ManagedDevice p = VLCTestPlayerFactory.build("127.0.0.1","11:11:11:11:11:11");
        //setUpDaemon("127.0.0.1");
//p.getCommand("restart").exec();
        p.stateReq().exec();
       // p.switchOffCmd().exec();
       // p.switchOnCmd().exec();
       // p.switchOffCmd().exec();

        logger.debug("INITED");
        Thread.sleep(200000);
    }

    @Test @Ignore
    public void player2() throws Exception {
        ((Handler)setUpServer("127.0.0.1",11212).getHandler()).isError = false;
        //((Handler)setUpServer("127.0.0.1",11211).getHandler()).isError = false;
        setUpDaemon("127.0.0.1");
Thread.sleep(1000);
        ManagedDevice p = VLCPlayer2ScreenFactory.build("127.0.0.1","11:11:11:11:11:11");
        p.stateReq().success.add((o)->logger.info("SUCCESSS"+o));

        p.stateReq().exec();
        p.switchOnCmd().exec();
        logger.debug("INITED");

        Thread.sleep(200000);
    }

    @Test
    public void testOn() throws Exception {
        ((Handler)setUpServer("127.0.0.1",11211).getHandler()).isError = false;
        ManagedDevice p = VLCPlayerFactory.buildDevice("127.0.0.1","11:11:11:11:11:11");
        p.stateReq().exec();
        logger.debug("INITED");
        Thread.sleep(200000);
    }

    @Test
    public void testOn1() throws Exception {
        ((Handler)setUpServer("127.0.0.1",11211).getHandler()).isError = false;
        ManagedDevice p = VLCPlayerFactory.buildDevice("127.0.0.1","11:11:11:11:11:11");
        p.switchOnCmd().exec();
        logger.debug("INITED");
        Thread.sleep(200000);
    }
}
