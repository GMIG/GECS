package org.gmig.gecs.factories;

import com.fasterxml.jackson.databind.JsonNode;
import org.gmig.gecs.command.Command;
import org.gmig.gecs.command.ComplexCommandBuilder;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.device.StateRequestResult;
import org.gmig.gecs.executors.TCPCommandExecutor;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Created by brix on 4/3/2018.
 */
public class VLCPlayer2ScreenFactory extends VLCPlayerFactory {

    private static final Logger logger = Logger.getLogger(VLCPlayer2ScreenFactory.class);

    private static TCPCommandExecutor statusRequestExecutor2 = new TCPCommandExecutor(textFilter,11212);
    protected static TCPCommandExecutor firstCheckExecutor2 = new TCPCommandExecutor(textFilter,11212);


    static {
        statusRequestExecutor2.setReconnectTimeMillis(statusRequestExecutor.getReconnectTimeMillis());
        statusRequestExecutor2.setMaxReconnectTries(statusRequestExecutor.getMaxReconnectTries());

        firstCheckExecutor2.setReconnectTimeMillis(firstCheckExecutor.getReconnectTimeMillis());
        firstCheckExecutor2.setMaxReconnectTries(firstCheckExecutor.getMaxReconnectTries());

    }
    @Override
    protected ManagedDevice buildType(JsonNode jsonNode, ManagedDevice.ManagedDeviceBuilder builder) throws IOException {
        return build(jsonNode.get("ip").asText(),jsonNode.get("mac").asText(),builder);
    }

    public static ManagedDevice build(String IP,String mac) throws IllegalArgumentException {
        return build(IP,mac, ManagedDevice.newBuilder().setName(IP));
    }

    public static ManagedDevice build(String IP, String mac, ManagedDevice.ManagedDeviceBuilder builder) throws IllegalArgumentException {

        Command<HashMap<String,?>> checkBoth1 = ComplexCommandBuilder.builder()
                .addCommand(0,"player1",(firstCheckExecutor.getCommand(IP,check)))
                .addCommand(0,"player2",(firstCheckExecutor2.getCommand(IP,check)))
                .collect(0);

        Command<StateRequestResult> stateInit1= ()->{
            CompletableFuture<StateRequestResult> future = new CompletableFuture<>();
            CompletableFuture<HashMap<String,?>> res= ComplexCommandBuilder.builder()
                    .addCommand(0,"player1",statusRequestExecutor.getCommand(IP,init))
                    .addCommand(0,"player2",statusRequestExecutor2.getCommand(IP,init))
                    .collect(0)
                    .get();
            res.handle((r,t)->{
                        if(t!=null){
                            future.completeExceptionally(t);
                            return null;
                        }
                        StateRequestResult f1 = ((StateRequestResult)r.get("player1"));
                        StateRequestResult f2 = ((StateRequestResult)r.get("player2"));
                        if(f1.isOn() && f2.isOn()) {
                            future.complete(StateRequestResult.IsOn(r));
                        }
                        else if(!f1.isOn() && !f2.isOn()) {
                            future.complete(StateRequestResult.IsOff(r));
                        }
                        else {
                            future.completeExceptionally(new Throwable("One of the vlc players error: 1:" + f1.returned() + ", 2:" + f2.returned()));
                        }
                        return null;
                    });
            return future;
        };

        ManagedDevice unit = buildClass(IP,mac,builder)
                .setFactory(VLCPlayer2ScreenFactory.class)
                .setStateRequestCommand(getInitWithPingCheck(stateInit1,IP))
                .setCheckCommand(checkBoth1).build();
        return unit;
    }
}
