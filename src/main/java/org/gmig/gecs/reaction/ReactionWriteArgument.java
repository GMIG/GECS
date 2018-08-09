package org.gmig.gecs.reaction;

import org.gmig.gecs.executors.SHelper;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSession;

import java.util.function.Function;

/**
 * Created by brix isOn 3/16/2018.
 */
public class ReactionWriteArgument extends Reaction {
    private Function<Object,Object>  msgCreator;
    public ReactionWriteArgument(Function<Object,Object> msgCreator) {
        this.msgCreator = msgCreator;
    }

    @Override
    protected IoFuture execute0(IoSession session, Object message){
        logger.debug(session.getRemoteAddress()+":Message sent:" + msgCreator.apply(SHelper.getArgument(session)));
        return session.write(msgCreator.apply(SHelper.getArgument(session)));
    }
}
