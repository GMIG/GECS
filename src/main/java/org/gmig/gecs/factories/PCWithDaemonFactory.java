package org.gmig.gecs.factories;

import com.fasterxml.jackson.databind.JsonNode;
import org.gmig.gecs.command.Command;
import org.gmig.gecs.command.ComplexCommandBuilder;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.executors.TCPCommandExecutor;
import org.gmig.gecs.executors.WOLCommandExecutor;
import org.gmig.gecs.reaction.Reaction;
import org.gmig.gecs.reaction.ReactionCloseWithSuccess;
import org.gmig.gecs.reaction.ReactionWrite;
import org.icmp4j.IcmpPingUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.gmig.gecs.executors.TCPReactionHandler.connectionTimeoutID;

/**
 * Created by brix on 9/3/2018.
 */
public class PCWithDaemonFactory extends AbstractManagedDeviceFactory {
    protected static int daemonPort = 10203;
    protected static final TCPCommandExecutor daemonCommandExecutor = new TCPCommandExecutor(textFilter,daemonPort);

    protected static int waitTimeForDaemonTurnOn = 30000;
    protected static int waitTimeForDaemonTurnOff = 20000;
    protected static int waitTimeForDaemonRestart = 30000;


    protected static final HashMap<Object,Reaction> daemonSwitchOff = Reaction.onConnectionTry(
            new ReactionWrite("SHUTDOWN")
                    .on("class java.io.IOException",
                            new ReactionCloseWithSuccess())
                    .on(connectionTimeoutID,
                            new ReactionCloseWithSuccess()),
            new ReactionCloseWithSuccess().setResultProcessor((o)->"device is off, shutdown not required"));
    protected static final HashMap<Object,Reaction> daemonRestart = Reaction.onConnectionSuccess(
            (new ReactionWrite("RESTART")
                    .on("class java.io.IOException",
                            new ReactionCloseWithSuccess())
                    .on(connectionTimeoutID,
                            new ReactionCloseWithSuccess())));
    protected static final HashMap<Object,Reaction> daemonCheck = Reaction.onConnectionSuccess(
            (new ReactionWrite("STATUS")
                    .on("1", new ReactionCloseWithSuccess())));

    protected static final HashMap<Object,Reaction> daemonInit = Reaction.onConnectionTry(
            (new ReactionWrite("STATUS")
                    .on("1", isOn)),isOff);

    protected  static Command<Void> getPingCheckAfterShutdown(String IP){
        return ()-> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            if (IcmpPingUtil.executePingRequest(IP,32,5000).getSuccessFlag())
                future.completeExceptionally(new Throwable("PC returns pings after shutdown request"));
            else
                future.complete(null);
            return future;
        };
    }

    protected static final WOLCommandExecutor wolExecutor = new WOLCommandExecutor();

    private static ManagedDevice buildPCWithDaemon(String IP, String mac, ManagedDevice.ManagedDeviceBuilder builder) throws IllegalArgumentException {
        return constructPCWithDaemon(IP,mac,builder).build();
    }
    @Override
    protected ManagedDevice buildType(JsonNode jsonNode, ManagedDevice.ManagedDeviceBuilder builder) throws IOException {
        return buildPCWithDaemon(jsonNode.get("ip").asText(),jsonNode.get("mac").asText(),builder);
    }
    static ManagedDevice.ManagedDeviceBuilder constructPCWithDaemon(String IP, String mac, ManagedDevice.ManagedDeviceBuilder builder) throws IllegalArgumentException {

        Command<?> turnOn = ComplexCommandBuilder.builder()
                .addCommand(0,"WoL",wolExecutor.getCommand(IP,0, mac).thenWait(waitTimeForDaemonTurnOn))
                .addCommand(1,"check", daemonCommandExecutor.getCommand(IP,daemonCheck))
                .build();
        Command<?> turnOff = ComplexCommandBuilder.builder()
                .addCommand(0,"turn off", daemonCommandExecutor.getCommand(IP, daemonSwitchOff).thenWait(waitTimeForDaemonTurnOff))
                .addCommand(1,"ping", getPingCheckAfterShutdown(IP))
                .build();
        Command<?> restartDaemmonWithCheck = ComplexCommandBuilder.builder()
                .addCommand(0,"restart", daemonCommandExecutor.getCommand(IP, daemonRestart).thenWait(waitTimeForDaemonTurnOff + waitTimeForDaemonTurnOn + waitTimeForDaemonRestart))
                .addCommand(1,"check", daemonCommandExecutor.getCommand(IP,daemonCheck))
                .build();


        return builder
                .setData("IP",IP)
                .setFactory(PCWithDaemonFactory.class)
                .setStateRequestCommand(daemonCommandExecutor.getCommand(IP, daemonInit))
                .setCheckCommand(daemonCommandExecutor.getCommand(IP, daemonCheck))
                .setSwitchOnCommand(turnOn)
                .setSwitchOffCommand(turnOff)
                .setCheckedRestartCommand(restartDaemmonWithCheck)
                .addCommand("check daemon", daemonCommandExecutor.getCommand(IP,daemonCheck))
                .addCommand("restart", daemonCommandExecutor.getCommand(IP, daemonRestart))
                .addCommand("ping after shutdown",getPingCheckAfterShutdown(IP))
                .setCheckResendTimeMinutes(1);
    }

    @Override
    public void dispose(){
        daemonCommandExecutor.dispose();
    }
}
