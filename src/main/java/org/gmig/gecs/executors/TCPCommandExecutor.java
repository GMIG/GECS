package org.gmig.gecs.executors;

import org.gmig.gecs.command.ArgCommand;
import org.gmig.gecs.command.Command;
import org.gmig.gecs.reaction.Reaction;
import org.apache.log4j.Logger;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LogLevel;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

public class TCPCommandExecutor {

    private static final Logger logger = Logger.getLogger(TCPCommandExecutor.class);

    private NioSocketConnector connector = new NioSocketConnector();
    private int port;
    private TCPReactionHandler handler;
    private int readTimeoutSecondsDefault = 5;

    private int maxReconnectTries = 5;
    private int reconnectTimeMillis = 2000;

    public int getReadTimeoutSeconds() {return connector.getSessionConfig().getReaderIdleTime();}

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {connector.getSessionConfig().setReaderIdleTime(readTimeoutSeconds);}

   public int getMaxReconnectTries() {return maxReconnectTries;}
    public void setMaxReconnectTries(int maxReconnectTries) {this.maxReconnectTries = maxReconnectTries;}
    public int getReconnectTimeMillis() {return reconnectTimeMillis;}
    public void setReconnectTimeMillis(int reconnectTimeMillis) {this.reconnectTimeMillis = reconnectTimeMillis;}


    public TCPCommandExecutor(ProtocolCodecFilter decodeFilter, int port) {
        this.port = port;
        try {
            connector.getFilterChain().addLast("decode", decodeFilter);
            connector.getFilterChain().addLast("log", new LoggingFilter("TCPCommandExecutor") {

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
            connector.getSessionConfig().setReaderIdleTime(readTimeoutSecondsDefault);
            connector.setConnectTimeoutMillis(5000);
            connector.getSessionConfig().setKeepAlive(false);
            handler = new TCPReactionHandler();
            connector.setHandler(handler);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T> Command<T> getCommand(String hostname, HashMap<Object,Reaction> reactionMap){
        return ()->submit(hostname,reactionMap);
    }
    public <U,T> ArgCommand<U,T> getArgCommand(String hostname, HashMap<Object,Reaction> reactionMap){
        return (o)->submit(hostname,reactionMap,o);
    }

    public <T>CompletableFuture<T> submit(String hostname,HashMap<Object,Reaction> reactionMap) {
        return submit(hostname,reactionMap,null);
    }
    HashMap<Object, Reaction> reactionMap;

    public <T> CompletableFuture<T> submit(String hostname, HashMap<Object, Reaction> reactionMap, Object argument) {
        CompletableFuture<T> returnedFuture = new CompletableFuture<>();
        //this.reactionMap = reactionMap;
        // handler.singleConnect(this.connector,hostname,port,returnedFuture,this.reactionMap);
        ConnectRetry retry = new ConnectRetry(() ->
        {
            ConnectFuture a = null;
            try {
                logger.debug(hostname + ":Connecting");
                a = connector.connect(new InetSocketAddress(hostname, port), (session, connectFuture) -> {
                    SHelper.setFields(session,reactionMap,hostname,returnedFuture,argument);
                });
            } catch (IllegalArgumentException e) {
                returnedFuture.completeExceptionally(e);
            }
            return a;
        }, ()->{
            logger.debug(hostname + ":Reconnect timeout");
            IoSession session = new DummySession();
            SHelper.setFields(session,reactionMap,hostname,returnedFuture,argument);
            handler.connectionFail(session);
            return null;
        }, hostname,maxReconnectTries,reconnectTimeMillis);
        return returnedFuture;
    }

    public void dispose(){
        connector.dispose();
    }

}
