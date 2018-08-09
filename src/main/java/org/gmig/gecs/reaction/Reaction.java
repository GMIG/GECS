package org.gmig.gecs.reaction;

import org.gmig.gecs.executors.TCPReactionHandler;
import org.apache.log4j.Logger;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSession;

import java.util.HashMap;

/**
 * Created by brix isOn 3/16/2018.
 */
public abstract class  Reaction {
    protected static final Logger logger = Logger.getLogger(Reaction.class);

    protected HashMap<Object, Reaction> nextMap = new HashMap<>();

    HashMap<Object, Reaction> peekNextMap() {
        return nextMap;
    }

    public static HashMap<Object, Reaction> onConnectionSuccess(Reaction r){
        HashMap<Object, Reaction> map = new HashMap<>();
        map.put(TCPReactionHandler.connectionCreatedID, r);
        return map;
    }

    public static HashMap<Object, Reaction> getMap(Object s, Reaction r){
        HashMap<Object, Reaction> map = new HashMap<>();
        map.put(s, r);
        return map;
    }

    public static HashMap<Object, Reaction> onConnectionFail(Reaction r){
        HashMap<Object, Reaction> map = new HashMap<>();
        map.put(TCPReactionHandler.connectionNotCreatedID, r);
        return map;
    }

    public static HashMap<Object, Reaction> onConnectionTry(Reaction connected, Reaction notConnected){
        HashMap<Object, Reaction> map = new HashMap<>();
        map.put(TCPReactionHandler.connectionCreatedID, connected);
        map.put(TCPReactionHandler.connectionNotCreatedID, notConnected);
        return map;
    }

    public HashMap<Object, Reaction>execute(IoSession session, Object message){
        IoFuture result = execute0(session,message);
        return nextMap;
    }

    protected abstract IoFuture execute0(IoSession session, Object message);

    public Reaction on(Object message, Reaction reaction) {
        nextMap.put(message, reaction);
        return this;
    }

    public Reaction addMap(HashMap<Object,Reaction> map) {
        nextMap.putAll(map);
        return this;
    }

    public Reaction thenAwaitClosing(Reaction reaction) {
        on(TCPReactionHandler.connectionClosedID, reaction);
        return this;
    }
}

