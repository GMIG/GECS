package org.gmig.gecs.command;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by brix on 4/4/2018.
 */
public class ComplexCommandBuilder {
    private static final Logger logger = Logger.getLogger(ComplexCommandBuilder.class);

    private ComplexCommandBuilder(){}

    public static ComplexCommandBuilder builder(){
        return new ComplexCommandBuilder();
    }

    private final LinkedHashMap<Integer,HashMap<String,Command<?>>> commandStructure = new LinkedHashMap<>();

    /*public ComplexCommandBuilder addCommand(int where,String id,Command<?> cmd){
        if (!commandStructure.containsKey(where)) {
            commandStructure.put(where, new HashMap<>());
        }
        commandStructure.get(where).put(id,cmd);
        return this;
    }*/
    public <T>ComplexCommandBuilder addCommand(int where,String id,Command<T> cmd){
        if (!commandStructure.containsKey(where)) {
            commandStructure.put(where, new HashMap<>());
        }
        commandStructure.get(where).put(id,cmd);
        return this;
    }
    private final LinkedHashMap<Integer, Command<HashMap<String, ?>>> cmds = new LinkedHashMap<>();


    private Command<HashMap<String,?>> join(Command<HashMap<String, ?>> old, int i, LinkedHashMap<Integer, HashMap<String, ?>> result){
        Command<HashMap<String,?>>add =  cmds.get(i);
        return ()->
                old.get().thenCompose((r)->{
                    result.put(i-1,r);
                    //logger.debug(result);
                    return add.get();
                });
    }

    // TODO: add collected commands order checks (here order 0-2 will produce an error)
    public Command <LinkedHashMap<Integer,HashMap<String,?>>> build() {
        return ()-> {
            CompletableFuture<LinkedHashMap<Integer, HashMap<String, ?>>> resultFuture = new CompletableFuture<>();
            LinkedHashMap<Integer, HashMap<String, ?>> total = new LinkedHashMap<>();
            for (int i = 0; i < commandStructure.size(); i++) {
                cmds.put(i,collect(i));
            }
            Command<HashMap<String, ?>> cmdnew = cmds.get(0);
            for(AtomicInteger i = new AtomicInteger(1); i.get()<cmds.size(); i.incrementAndGet()){
                cmdnew = join(cmdnew, i.get(), total);
            }
            CompletableFuture<HashMap<String,?>> getFuture = cmdnew.get();
            getFuture.handle((r,t) -> {
                if (t!=null){
                    resultFuture.completeExceptionally(t);
                }
                else {
                    total.put(commandStructure.size() - 1, r);
                    resultFuture.complete(total);
                }
                return null;
            });
            return resultFuture;
        };
    }

    public Command <HashMap<String,?>> collect(int i) {
        return ()->{
            CompletableFuture<HashMap<String, ?>> resultFuture = new CompletableFuture<>();
            HashMap<String, Object> result = new HashMap<>();
            CompletableFuture<Void> futuresWithtinMap = CompletableFuture.completedFuture(null);
            for (HashMap.Entry<String, Command<?>> entry : commandStructure.get(i).entrySet()) {
                CompletableFuture<?> fut = entry.getValue().get();
                futuresWithtinMap = futuresWithtinMap.thenCombine(fut, (nul, o) -> {
                    logger.debug("ComplexCmd-putting to:" + entry.getKey() + " " + o);
                    result.put(entry.getKey(), o);
                        return null;
                });
            }
            futuresWithtinMap.handle((o,t) -> {
                if (t!=null) {
                    logger.debug("ComplexCmd-throwing:" + result + " " + t);
                    resultFuture.completeExceptionally(t);
                }
                else {
                    logger.debug("ComplexCmd-completing with:"+ result);
                    resultFuture.complete(result);
                }
                return null;
            });
            return resultFuture;
        };
    }
    @SuppressWarnings("SameParameterValue")
    public Command <HashMap<String,?>> parallel(int i) {
        return ()->{
            HashMap<String, Object> result = new HashMap<>();
            HashSet<CompletableFuture<?>> futuresSet = new HashSet<>();
            for (HashMap.Entry<String, Command<?>> entry : commandStructure.get(i).entrySet()) {
                CompletableFuture<?> fut = entry.getValue().get();
                futuresSet.add(fut.whenComplete((o,t)-> {
                    if (t != null)
                        result.put(entry.getKey(), t);
                    else
                        result.put(entry.getKey(), o);
                }));
            }
            CompletableFuture<HashMap<String, ?>> resultFuture = new CompletableFuture<>();

            CompletableFuture.allOf(futuresSet.toArray(new CompletableFuture[futuresSet.size()])).whenComplete((o,t)-> resultFuture.complete(result));
            return resultFuture;
        };
    }

}