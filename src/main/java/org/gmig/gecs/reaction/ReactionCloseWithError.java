package org.gmig.gecs.reaction;

import org.gmig.gecs.executors.SHelper;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSession;

import java.util.concurrent.CompletableFuture;

/**
 * Created by brix isOn 3/16/2018.
 */
public class ReactionCloseWithError extends ReactionClose {
    private String error;
    public ReactionCloseWithError(String error) {
        this.error = (error);
    }
    @Override
    protected IoFuture execute0(IoSession session, Object message){
        IoFuture f =super.execute0(session,message);
        f.addListener((ss)-> {
            logger.warn(session.getRemoteAddress() + ":Command exception:" + error);
            CompletableFuture future = SHelper.getFuture(session);
            String host = session.getRemoteAddress().toString();
            if(host.equals("?"))
                host = SHelper.getIP(session);
            future.completeExceptionally(new IllegalStateException(host+":Command exception:" + error));
        });
        return f;
    }

}
