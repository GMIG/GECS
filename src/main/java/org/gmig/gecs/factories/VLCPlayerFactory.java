package org.gmig.gecs.factories;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.log4j.Logger;
import org.gmig.gecs.command.Command;
import org.gmig.gecs.command.ComplexCommandBuilder;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.device.StateRequestResult;
import org.gmig.gecs.executors.TCPCommandExecutor;
import org.gmig.gecs.reaction.*;
import org.icmp4j.IcmpPingUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Created by brix isOn 3/16/2018.
 */
@SuppressWarnings("WeakerAccess")
public class VLCPlayerFactory extends PCWithDaemonFactory {

    protected static final Logger logger = Logger.getLogger(VLCPlayerFactory.class);

    protected static int vlcPlayerDefaultPort = 11211;
    protected static int daemonPort = 10203;
    protected static final TCPCommandExecutor statusRequestExecutor = new TCPCommandExecutor(textFilter,vlcPlayerDefaultPort);
    protected static final TCPCommandExecutor firstCheckExecutor = new TCPCommandExecutor(textFilter,vlcPlayerDefaultPort);
    protected static final TCPCommandExecutor regularCheckExecutor = new TCPCommandExecutor(textFilter,vlcPlayerDefaultPort);

    protected static int waitTimeForTurnOn = 35000;
    protected static int waitTimeForTurnOff = 20000;
    protected static int waitTimeForRestart = 20000;


    //protected static final WOLCommandExecutor wolExecutor = new WOLCommandExecutor();
    protected static final HashMap<Object, Reaction> error = new HashMap<>();

    static {
        error.put("1", new ReactionCloseWithError("Cannot load settings.json file"));
        error.put("3", new ReactionCloseWithError("Display parameters changed"));
        error.put("4", new ReactionCloseWithError("No required display to connect. Waiting"));
        error.put("5", new ReactionCloseWithError("External COM device not found"));
        error.put("6", new ReactionCloseWithError("Media loading or playing error"));
        error.put("7", new ReactionCloseWithError("Required audio device not found"));
        error.put("9", new ReactionCloseWithError("Unknown error"));

        statusRequestExecutor.setReconnectTimeMillis(6000);
        statusRequestExecutor.setMaxReconnectTries(2);

        firstCheckExecutor.setReconnectTimeMillis(5000);
        firstCheckExecutor.setMaxReconnectTries(5);

        regularCheckExecutor.setReconnectTimeMillis(2000);
        regularCheckExecutor.setMaxReconnectTries(2);

        wolExecutor.setIpResendTimeMillis(10000);
        wolExecutor.setIpResendTries(5);
    }
    protected static final HashMap<Object,Reaction> init = Reaction.onConnectionTry(
            new ReactionDoNothing().on("CONNECTED",new ReactionWrite("STATUS")
                    .on("0",isOn)
                    .on("1",isOn)
                    .on("2",new ReactionWrite("ERROR").addMap(error)))
            ,
            isOff);

    protected static final HashMap<Object,Reaction> check = Reaction.onConnectionSuccess(
            new ReactionDoNothing().on("CONNECTED",new ReactionWrite("STATUS")
                    .on("0",new ReactionCloseWithSuccess())
                    .on("1",new ReactionCloseWithSuccess())
                    .on("2",new ReactionWrite("ERROR").addMap(error))));

    protected static final HashMap<Object,Reaction> play = Reaction.onConnectionSuccess(
            new ReactionDoNothing().on("CONNECTED",new ReactionWrite("PLAY")
                    .on("0",new ReactionCloseWithSuccess())));

    protected static final HashMap<Object,Reaction> mute = Reaction.onConnectionSuccess(
            new ReactionDoNothing().on("CONNECTED",new ReactionWrite("MUTE")
                    .on("0",new ReactionCloseWithSuccess())));


    @Override
    protected ManagedDevice buildType(JsonNode jsonNode, ManagedDevice.ManagedDeviceBuilder builder) throws IOException {
        return buildVLCPlayer(jsonNode.get("ip").asText(),jsonNode.get("mac").asText(),builder);
    }

    @Override
    public void dispose() {
         statusRequestExecutor.dispose();
         firstCheckExecutor.dispose();
         regularCheckExecutor.dispose();
         wolExecutor.dispose();
    }

    public static ManagedDevice buildDevice(String IP, String mac) throws IllegalArgumentException {
        return buildVLCPlayer(IP,mac, ManagedDevice.newBuilder().setName(IP));
    }

    protected static Command<StateRequestResult> getInitWithPingCheck(Command<StateRequestResult> cmd, String IP) {
        return () -> {
            CompletableFuture<StateRequestResult> future = new CompletableFuture<>();
            cmd.get().handleAsync((r,t) -> {
                        if(t != null)
                            future.completeExceptionally(t);
                        if (!(r).isOn() &&
                                IcmpPingUtil.executePingRequest(IP, 32, 5000).getSuccessFlag())
                            future.completeExceptionally(new Throwable("PC returns pings but player is disconnected"));
                        else
                            future.complete(r);
                        return r;
                    });
            return future;
        };
    }

    public static ManagedDevice.ManagedDeviceBuilder constructVLCPlayer(String IP, String mac, ManagedDevice.ManagedDeviceBuilder builder) throws IllegalArgumentException {


        Command<?> turnOn = ComplexCommandBuilder.builder()
                .addCommand(0,"WoL",wolExecutor.getCommand(IP,0,mac).thenWait(waitTimeForTurnOn))
                .addCommand(1,"check", firstCheckExecutor.getCommand(IP,check))
                .build();
        HashMap<Object,Reaction> adjust = Reaction.onConnectionSuccess(
                new ReactionDoNothing().on("CONNECTED",new ReactionWrite("ADJUST")
                .on("1", new ReactionCloseWithSuccess())));

        Command<?> turnOff = ComplexCommandBuilder.builder()
                .addCommand(0,"turn off", daemonCommandExecutor.getCommand(IP, daemonSwitchOff).thenWait(waitTimeForTurnOff))
                .addCommand(1,"ping", getPingCheckAfterShutdown(IP))
                .build();

        Command<?> restartWithCheck = ComplexCommandBuilder.builder()
                .addCommand(0,"restart", daemonCommandExecutor.getCommand(IP, daemonRestart).thenWait(waitTimeForTurnOff + waitTimeForTurnOn + waitTimeForRestart))
                .addCommand(1,"check", firstCheckExecutor.getCommand(IP,check))
                .build();

        return constructPCWithDaemon(IP,mac,builder)
                .setData("IP",IP)
                .setFactory(VLCPlayerFactory.class)
                .setStateRequestCommand(getInitWithPingCheck(statusRequestExecutor.getCommand(IP,init),IP).thenWait(10000))
                .setCheckedRestartCommand(restartWithCheck)
                .addCommand("adjust",regularCheckExecutor.getCommand(IP,adjust))
                .setSwitchOnCommand(turnOn)
                .setSwitchOffCommand(turnOff)
                .addCommand("play",statusRequestExecutor.getCommand(IP,play))
                .addCommand("mute",statusRequestExecutor.getCommand(IP,mute))
                .addCommand("send wol",wolExecutor.getCommand(IP,0,mac))
                .setCheckCommand(regularCheckExecutor.getCommand(IP,check))
                .setCheckResendTimeMinutes(1);
    }
    public static ManagedDevice buildVLCPlayer(String IP, String mac, ManagedDevice.ManagedDeviceBuilder builder) throws IllegalArgumentException {
        return constructVLCPlayer(IP,mac,builder).build();
    }

}
