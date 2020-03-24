package org.gmig.gecs.reaction;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSession;
import org.gmig.gecs.executors.SHelper;
import org.gmig.gecs.executors.TCPReactionHandler;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Created by brix isOn 3/16/2018.
 */
public abstract class ReactionClose extends Reaction {
    private ConcurrentLinkedQueue<Consumer<Object>> callbacks = new ConcurrentLinkedQueue<>();
    private Executor executor = Executors.newCachedThreadPool();

    public ReactionClose afterCompletionAccept(Consumer<Object> action){
        callbacks.add(action);
        return this;
    }


    @Override
    protected IoFuture execute0(IoSession session, Object message){
        nextMap.put(TCPReactionHandler.connectionClosedID, new ReactionDoNothing());
        callbacks.forEach((c)-> SHelper.getFuture(session).thenAcceptAsync(c,executor));
        return session.closeNow();
    }

}
