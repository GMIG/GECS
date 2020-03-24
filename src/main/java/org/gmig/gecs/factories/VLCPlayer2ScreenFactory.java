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
    private static int vlcPlayer2Port = 11212;
    private static int vlcPlayer3Port = 11216;

    private static final TCPCommandExecutor statusRequestExecutor2 = new TCPCommandExecutor(textFilter,vlcPlayer2Port);
    private static final TCPCommandExecutor firstCheckExecutor2 = new TCPCommandExecutor(textFilter,vlcPlayer2Port);

    //TODO: REFACTOR THIS HACK
    private static final TCPCommandExecutor commandExecutor3 = new TCPCommandExecutor(textFilter,vlcPlayer3Port);

    static {
        statusRequestExecutor2.setReconnectTimeMillis(statusRequestExecutor.getReconnectTimeMillis());
        statusRequestExecutor2.setMaxReconnectTries(statusRequestExecutor.getMaxReconnectTries());
        firstCheckExecutor2.setReconnectTimeMillis(firstCheckExecutor.getReconnectTimeMillis());
        firstCheckExecutor2.setMaxReconnectTries(firstCheckExecutor.getMaxReconnectTries());
        commandExecutor3.setReconnectTimeMillis(firstCheckExecutor.getReconnectTimeMillis());
        commandExecutor3.setMaxReconnectTries(firstCheckExecutor.getMaxReconnectTries());

    }
    @Override
    protected ManagedDevice buildType(JsonNode jsonNode, ManagedDevice.ManagedDeviceBuilder builder) throws IOException {
        return buildVLCPlayer2Screen(jsonNode.get("ip").asText(),jsonNode.get("mac").asText(),builder);
    }
    @Override
    public void dispose() {
        super.dispose();
        statusRequestExecutor2.dispose();
        firstCheckExecutor2.dispose();
    }


    public static ManagedDevice build(String IP,String mac) throws IllegalArgumentException {
        return buildVLCPlayer2Screen(IP,mac, ManagedDevice.newBuilder().setName(IP));
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static ManagedDevice buildVLCPlayer2Screen(String IP, String mac, ManagedDevice.ManagedDeviceBuilder builder) throws IllegalArgumentException {

        Command<HashMap<String,?>> checkBoth1 = ComplexCommandBuilder.builder()
                .addCommand(0,"player1",(firstCheckExecutor.getCommand(IP,check)))
                .addCommand(0,"player2",(firstCheckExecutor2.getCommand(IP,check)))
                .collect(0);

        Command<?> turnOn = ComplexCommandBuilder.builder()
                .addCommand(0,"WoL",wolExecutor.getCommand(IP,0,mac).thenWait(waitTimeForTurnOn+10000))
                .addCommand(1,"check", checkBoth1)
                .build();

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
        Command<?> restartWithCheck = ComplexCommandBuilder.builder()
                .addCommand(0,"restart", daemonCommandExecutor.getCommand(IP, daemonRestart).thenWait(waitTimeForTurnOff + waitTimeForTurnOn + waitTimeForRestart*2))
                .addCommand(1,"check", checkBoth1)
                .build();


        ManagedDevice unit = constructVLCPlayer(IP,mac,builder)
                .setFactory(VLCPlayer2ScreenFactory.class)
                .setSwitchOnCommand(turnOn)
                .setStateRequestCommand(getInitWithPingCheck(stateInit1,IP))
                .setCheckedRestartCommand(restartWithCheck)
                .addCommand("play",commandExecutor3.getCommand(IP,play))
                .setCheckCommand(checkBoth1)
                .build();
        return unit;
    }
}
