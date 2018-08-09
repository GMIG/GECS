package org.gmig.gecs.executors;

import org.gmig.gecs.device.Device;
import org.gmig.gecs.reaction.Reaction;
import org.gmig.gecs.reaction.ReactionDoNothing;
import org.apache.log4j.Logger;
import org.apache.mina.core.session.DefaultIoSessionDataStructureFactory;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionAttributeMap;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LogLevel;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

/**
 * Created by brix on 8/1/2018.
 */
public class TCPCommandAcceptor {

    public final HashSet<Device> sources = new HashSet<>();

    private static final Logger logger = Logger.getLogger(TCPCommandAcceptor.class);
    private int maxReconnectTries = 5;
    private int reconnectTimeMillis = 2000;
    private int readTimeoutSecondsDefault = 5;


    private NioSocketAcceptor acceptor = new NioSocketAcceptor();
    private int port;
    private TCPReactionHandler handler;
    ReactionDoNothing root = new ReactionDoNothing();

    public TCPCommandAcceptor(ProtocolCodecFilter decodeFilter, int port) {
        this.port = port;
        try {
            acceptor.getFilterChain().addLast("decode", decodeFilter);
            acceptor.getFilterChain().addLast("log", new LoggingFilter("TCPCommandAcceptor") {

                //This is to disable verbose java.io.IOException stack trace prints
                @Override
                public void exceptionCaught(NextFilter nextFilter, IoSession session, Throwable cause) throws Exception {
                    if (cause instanceof java.io.IOException) {
                        LogLevel ll = this.getExceptionCaughtLogLevel();
                        this.setExceptionCaughtLogLevel(LogLevel.NONE);
                        super.exceptionCaught(nextFilter, session, cause);
                        this.setExceptionCaughtLogLevel(ll);
                    } else
                        super.exceptionCaught(nextFilter, session, cause);
                }
            });
            //acceptor.getSessionConfig().setReaderIdleTime(readTimeoutSecondsDefault);
            acceptor.getSessionConfig().setKeepAlive(false);
            handler = new TCPReactionHandler();
            String statusReq = "(.*):active";
            HashMap<Object,Reaction> defaultMap = Reaction.onConnectionSuccess(root);
/*                    new ReactionDoNothing()
                                    .on(Pattern.compile(statusReq),new ReactionCloseWithSuccess()
                                            .setResultProcessor((o)->{
                                                String str = (String) o;
                                                logger.debug("I am activated");
                                                return null;
                                            })));*/

            DefaultIoSessionDataStructureFactory factory = new DefaultIoSessionDataStructureFactory(){
                public IoSessionAttributeMap getAttributeMap(IoSession session){
                    IoSessionAttributeMap map = null;
                    try {
                        map = super.getAttributeMap(session);
                        map.setAttribute(session, SHelper.Fields.ReactionMap,defaultMap);
                        map.setAttribute(session, SHelper.Fields.ChainFuture,new CompletableFuture<>());
                        map.setAttribute(session, SHelper.Fields.IP,"");
                        map.setAttribute(session, SHelper.Fields.Argument,"");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return map;
                }
            };
            acceptor.setSessionDataStructureFactory(factory);
            acceptor.setHandler(handler);
            acceptor.bind(new InetSocketAddress(port));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addRule(HashMap<Object,Reaction> rule){
        root.addMap(rule);
    }

    public void addRule(Object o, Reaction r){
        HashMap<Object,Reaction> map = new HashMap<>();
        map.put(o,r);
        addRule(map);
    }
/*
    public void addRule(String Source, String signal, Function<Object,Object> action){
        ReactionCloseWithSuccess r = new ReactionCloseWithSuccess()
                .setResultProcessor(action);
        addRule(Source + ":" + signal,r);
    }

    public void addSource(Device device){


        device.getArgCommandList().forEach((name,command)->{
            addRule(command.getName(),new ReactionCloseWithSuccess()
                    .afterCompletionAccept((o)->command.exec(o).join()));
        });

    }*/

}
