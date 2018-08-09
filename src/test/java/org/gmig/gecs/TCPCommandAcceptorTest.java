package org.gmig.gecs;

import org.gmig.gecs.device.Device;
import org.gmig.gecs.executors.TCPCommandAcceptor;
import org.gmig.gecs.reaction.ReactionCloseWithSuccess;
import org.apache.log4j.Logger;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;
import org.apache.mina.filter.codec.textline.TextLineEncoder;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by brix on 8/1/2018.
 */
public class TCPCommandAcceptorTest {

    private static final Logger logger = Logger.getLogger(TCPCommandAcceptorTest.class);


    private ProtocolCodecFilter textfilter = new ProtocolCodecFilter(
            new TextLineEncoder(),
            new TextLineDecoder());

    @Test
    public void done() throws Exception {
        int port = 11213;
        AtomicBoolean b = new AtomicBoolean(false);
        TCPCommandAcceptor a = new TCPCommandAcceptor(textfilter,port);
        a.addRule(Pattern.compile("(.*):active"),new ReactionCloseWithSuccess()
                .setResultProcessor((o)->{
                    String str = (String) o;
                    b.set(true);
                    logger.debug("I am activated");
                    return null;
                }));
        Thread.sleep(1000);
        Device d=Device.builder().build();

        Device.builder().addCommand("d",d.getCommand("d").getCommand());
        NioSocketConnector con = new NioSocketConnector();
        con.getFilterChain().addLast("dec",textfilter);
        con.setHandler(new IoHandlerAdapter(){
            @Override
            public void sessionOpened(IoSession session) throws Exception{
                session.write("SE-28:active");
            }
        });
        con.connect(new InetSocketAddress("127.0.0.1", port));
        Thread.sleep(2000);
        assertTrue(b.get());
    }

    @Test
    public void notdone() throws Exception {
        int port = 11213;
        AtomicBoolean b = new AtomicBoolean(false);
        TCPCommandAcceptor a = new TCPCommandAcceptor(textfilter,port);
        a.addRule(Pattern.compile("(.*):active"),new ReactionCloseWithSuccess()
                .setResultProcessor((o)->{
                    String str = (String) o;
                    b.set(true);
                    logger.debug("I am activated");
                    return null;
                }));
        Thread.sleep(1000);

        NioSocketConnector con = new NioSocketConnector();
        con.getFilterChain().addLast("dec",textfilter);
        con.setHandler(new IoHandlerAdapter(){
            @Override
            public void sessionOpened(IoSession session) throws Exception{
                session.write("SE-28:lactive");
            }
        });
        con.connect(new InetSocketAddress("127.0.0.1", port));
        Thread.sleep(2000);
        assertFalse(b.get());
    }
}