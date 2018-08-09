package org.gmig.gecs.device;

import org.gmig.gecs.command.ListenableCommand;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by brix on 4/17/2018.
 */
public  interface Switchable <U,T>{
    ListenableCommand<U> switchOnCmd();
    ListenableCommand<T> switchOffCmd();
    String getName();
    Set<Switchable> getChildren();
    //ListenableCommand<?> getCommand(String ID);
    default Set<Switchable> getAllChildren(){
        Set<Switchable> leafNodes = new HashSet<>();
        if (this.getChildren().isEmpty()) {
            leafNodes.add(this);
        } else{
            Set<Switchable> children = this.getChildren();
            for (Switchable<?,?> child : children) {
                leafNodes.addAll(child.getAllChildren());
            }
        }
        return leafNodes;
    }


}
