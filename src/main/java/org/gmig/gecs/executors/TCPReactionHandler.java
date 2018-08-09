package org.gmig.gecs.executors;

import org.gmig.gecs.reaction.Reaction;
import org.gmig.gecs.reaction.ReactionCloseWithError;
import org.apache.log4j.Logger;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 *
 */
public class TCPReactionHandler extends IoHandlerAdapter  {

    public boolean setException = false;

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        if(setException) {
            Thread.sleep(2000);
            throw new IOException("Connect exception test");
        }
    }

    public static final String connectionClosedID = "Connection closed";
    public static final String connectionTimeoutID = "Client became idle";
    public static final String connectionCreatedID = "Connection created";
    public static final String connectionNotCreatedID = "Client not connected";

    private static final Logger logger = Logger.getLogger(TCPReactionHandler.class);

    private synchronized void processEvent(IoSession session, Object message){
            HashMap<Object, Reaction> activeMap = SHelper.getReactionMap(session);
            logger.debug(session.getRemoteAddress()+":Message received:" + message);
            Reaction reaction;
            Optional<Map.Entry<Object,Reaction>> regexReaction= activeMap.entrySet().stream()
                .filter((o)->o.getKey().getClass()==Pattern.class && message.getClass() == String.class)
                    .filter((o)->((Pattern)o.getKey()).matcher((String) message).matches())
                    .findFirst();

            if (activeMap.containsKey(message)) {
                reaction = activeMap.get(message);
            } else if(regexReaction.isPresent()){
                reaction = regexReaction.get().getValue();
            }
            else {
                reaction = new ReactionCloseWithError("Unexpected action from client:" + message);
            }
            HashMap<Object, Reaction> newMap = reaction.execute(session,message);
            SHelper.setReactionMap(session, newMap);
    }

    @Override
    public void sessionClosed(IoSession session) {
        processEvent(session,connectionClosedID);}
    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        processEvent(session, connectionTimeoutID);}
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) {
        processEvent(session,cause.getClass().toString());}
    @Override
    public void messageReceived(IoSession session, Object message){
        processEvent(session,message);}
    @Override
    public void sessionOpened(IoSession session) throws InterruptedException {
        processEvent(session,connectionCreatedID);}

    public void connectionFail(IoSession session) {
        processEvent(session,connectionNotCreatedID);
    }


    void singleConnect(NioSocketConnector c, String hostname, int port, CompletableFuture<?> chainFuture, HashMap<Object, Reaction> firstReactionMap){
        ConnectFuture future = c.connect(new InetSocketAddress(hostname, port));

        future.awaitUninterruptibly();
        IoSession session;
        String event;

        if (!future.isConnected()) {
            logger.debug("Connect timeout exception");
            session = new DummySession();
            event = connectionNotCreatedID;
        }
        else {
            logger.debug("Connect success");
            session = future.getSession();
            event = connectionCreatedID;
        }
        SHelper.initFields(session);
        SHelper.setReactionMap(session,firstReactionMap);
        SHelper.setIP(session,hostname);
        SHelper.setFuture(session,chainFuture);
        processEvent(session,event);
    }


}


