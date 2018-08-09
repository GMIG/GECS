package org.gmig.gecs.reaction;

import org.gmig.gecs.executors.SHelper;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSession;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Created by brix isOn 3/16/2018.
 */
public class ReactionCloseWithSuccess extends ReactionClose {
    protected Function<Object,Object> strategy = (o)->o;

    public ReactionCloseWithSuccess setResultProcessor(Function<Object,Object> startegy){
        this.strategy = startegy;
        return this;
    }

    @Override
    protected IoFuture execute0(IoSession session, Object message){
        IoFuture f =super.execute0(session,message);
        f.addListener((ss)-> {
            logger.debug(session.getRemoteAddress() + ":Command OK:" + message);
            CompletableFuture<Object> future = SHelper.getFuture(session);
            future.complete(strategy.apply(message));
        });
        return f;
    }
}
