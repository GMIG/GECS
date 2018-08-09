package org.gmig.gecs.reaction;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSession;

/**
 * Created by brix isOn 3/16/2018.
 */
public class ReactionWrite extends Reaction {
    private Object sendMessage;
    public ReactionWrite(Object message) {
        this.sendMessage = message;
    }

    @Override
    protected IoFuture execute0(IoSession session, Object message){
        logger.debug(session.getRemoteAddress()+":Message sent:" + sendMessage);
        return session.write(sendMessage);
    }
}
