package org.gmig.gecs.reaction;

import org.apache.mina.core.future.DefaultIoFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.session.IoSession;

/**
 * Created by brix isOn 3/16/2018.
 */
public class ReactionDoNothing extends Reaction {
    public ReactionDoNothing(){
        //super((session)->{},false);
    }
    @Override
    protected IoFuture execute0(IoSession session, Object message){
        DefaultIoFuture f = new DefaultIoFuture(session);
        f.setValue(null);
        return f;
    }
}
