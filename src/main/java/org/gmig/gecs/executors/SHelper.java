package org.gmig.gecs.executors;

import org.gmig.gecs.reaction.Reaction;
import org.apache.mina.core.session.IoSession;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Created by brix isOn 2/20/2018.
 */


public class SHelper {
    public enum Fields{
        ReactionMap,
        ChainFuture,
        Context,
        IP,
        Argument
    }

    public static synchronized IoSession initFields( IoSession ioSession){
        ioSession.setAttribute(Fields.ReactionMap,
                new HashMap<Object, Reaction>());
        ioSession.setAttribute(Fields.IP, "");
        ioSession.setAttribute(Fields.Argument, null);
        return ioSession;
    }
    public static synchronized <T>IoSession  setFields( IoSession ioSession,
                                                        HashMap<Object, Reaction> reactionMap,
                                                        String hostname,
                                                        CompletableFuture <T>  returnedFuture,
                                                        Object argument){
        SHelper.initFields(ioSession);
        SHelper.setReactionMap(ioSession,reactionMap);
        SHelper.setIP(ioSession,hostname);
        SHelper.setFuture(ioSession,returnedFuture);
        SHelper.setArgument(ioSession,argument);
        return ioSession;
    }

    public static synchronized HashMap<Object, Reaction> getReactionMap( IoSession ioSession) throws ClassCastException{
        Object currentMapObj = ioSession.getAttribute(Fields.ReactionMap);
        if (currentMapObj == null)
            throw new ClassCastException("Reaction map not set");

        if(!(currentMapObj instanceof HashMap))
            throw new ClassCastException();

        return (HashMap<Object, Reaction>)currentMapObj;
    }

    public static synchronized void setReactionMap( IoSession ioSession, HashMap<Object, Reaction> map){
        ioSession.setAttribute(Fields.ReactionMap,map);
    }


    public static synchronized <T> CompletableFuture <T>getFuture( IoSession ioSession) throws ClassCastException{
        Object currentMapObj = ioSession.getAttribute(Fields.ChainFuture);
        if (currentMapObj != null && (currentMapObj instanceof CompletableFuture ))
            return (CompletableFuture<T>)currentMapObj;
        throw new ClassCastException();
    }

    public static synchronized <T>void setFuture( IoSession ioSession, CompletableFuture<T> future){
            ioSession.setAttribute(Fields.ChainFuture,future);
    }

    public static synchronized String getIP( IoSession ioSession) throws ClassCastException{
        Object IP = ioSession.getAttribute(Fields.IP);
        if (IP != null && (IP instanceof String))
            return (String)IP;
        throw new ClassCastException();
    }

    public static synchronized void setIP( IoSession ioSession, String IP){
        ioSession.setAttribute(Fields.IP,IP);
    }

    public static synchronized Object getArgument( IoSession ioSession) throws ClassCastException{
        Object arg = ioSession.getAttribute(Fields.Argument);
        if (arg != null)
            return arg;
        throw new ClassCastException();
    }

    public static synchronized <T>void setArgument(IoSession ioSession, Object arg){
        ioSession.setAttribute(Fields.Argument,arg);
    }


}
