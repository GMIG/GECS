package org.gmig.gecs.factories;

import com.fasterxml.jackson.databind.JsonNode;
import org.gmig.gecs.command.Command;
import org.gmig.gecs.command.ComplexCommandBuilder;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.device.StateRequestResult;
import org.gmig.gecs.executors.TCPCommandExecutor;
import org.gmig.gecs.executors.WOLCommandExecutor;
import org.apache.log4j.Logger;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;
import org.apache.mina.filter.codec.textline.TextLineEncoder;
import org.gmig.gecs.reaction.*;
import org.icmp4j.IcmpPingUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Created by brix isOn 3/16/2018.
 */
public class VLCPlayerFactory extends ManagedDeviceFactory {

    protected static final Logger logger = Logger.getLogger(VLCPlayerFactory.class);

    protected static ProtocolCodecFilter textFilter = new ProtocolCodecFilter(
            new TextLineEncoder(Charset.forName("UTF-8"), LineDelimiter.MAC),new TextLineDecoder(Charset.forName("UTF-8"), LineDelimiter.MAC));

    protected static TCPCommandExecutor statusRequestExecutor = new TCPCommandExecutor(textFilter,11211);
    protected static TCPCommandExecutor firstCheckExecutor = new TCPCommandExecutor(textFilter,11211);
    protected static TCPCommandExecutor regularCheckExecutor = new TCPCommandExecutor(textFilter,11211);

    protected static TCPCommandExecutor powerCommandExecutor = new TCPCommandExecutor(textFilter,10203);
    protected static WOLCommandExecutor wolExecutor = new WOLCommandExecutor();
    static HashMap<Object, Reaction> error = new HashMap<>();

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
        firstCheckExecutor.setMaxReconnectTries(3);

        regularCheckExecutor.setReconnectTimeMillis(2000);
        regularCheckExecutor.setMaxReconnectTries(2);

        wolExecutor.setIpResendTimeMillis(10000);
        wolExecutor.setIpResendTries(5);
    }
    static HashMap<Object,Reaction> init = Reaction.onConnectionTry(
            new ReactionDoNothing().on("CONNECTED",new ReactionWrite("STATUS")
                    .on("0",isOn)
                    .on("1",isOn)
                    .on("2",new ReactionWrite("ERROR").addMap(error)))
            ,
            isOff);

    static HashMap<Object,Reaction> check = Reaction.onConnectionSuccess(
            new ReactionDoNothing().on("CONNECTED",new ReactionWrite("STATUS")
                    .on("0",new ReactionCloseWithSuccess())
                    .on("1",new ReactionCloseWithSuccess())
                    .on("2",new ReactionWrite("ERROR").addMap(error))));

    static HashMap<Object,Reaction> play = Reaction.onConnectionSuccess(
            new ReactionDoNothing().on("CONNECTED",new ReactionWrite("PLAY")
                    .on("0",new ReactionCloseWithSuccess())));


    static HashMap<Object,Reaction> powerOff = Reaction.onConnectionTry(
            new ReactionWrite("SHUTDOWN")
                    .on("class java.io.IOException",
                            new ReactionCloseWithSuccess()),
            new ReactionCloseWithSuccess());
    static HashMap<Object,Reaction> powerRestart = Reaction.onConnectionSuccess(
            (new ReactionWrite("RESTART")
                    .on("class java.io.IOException",
                            new ReactionCloseWithSuccess())));

    @Override
    protected ManagedDevice buildType(JsonNode jsonNode, ManagedDevice.ManagedDeviceBuilder builder) throws IOException {
        return buildDevice(jsonNode.get("ip").asText(),jsonNode.get("mac").asText(),builder);
    }
    public static ManagedDevice buildDevice(String IP, String mac) throws IllegalArgumentException {
        return buildDevice(IP,mac, ManagedDevice.newBuilder().setName(IP));
    }

    protected static Command<StateRequestResult> getInitWithPingCheck(Command<StateRequestResult> cmd, String IP){
        return ()->{
            CompletableFuture<StateRequestResult> future = new CompletableFuture<>();
            cmd.thenWait(10000).get().thenApplyAsync((r)->{
                if(!(r).isOn() &&
                        IcmpPingUtil.executePingRequest(IP,32,5000).getSuccessFlag())
                    future.completeExceptionally(new Throwable("PC returns pings but daemon is disconnected"));
                else
                    future.complete(r);
                return r;
            });
            return future;
        };
    }

    public static ManagedDevice.ManagedDeviceBuilder buildClass(String IP, String mac, ManagedDevice.ManagedDeviceBuilder builder) throws IllegalArgumentException {

        Command<?> turnOn = ComplexCommandBuilder.builder()
                .addCommand(0,"WoL",wolExecutor.getCommand(IP,0,mac).thenWait(10000))
                .addCommand(1, "check", firstCheckExecutor.getCommand(IP,check)).build();

        Command<Void> ping = ()-> {
            CompletableFuture<Void> future = new CompletableFuture<>();
             if (IcmpPingUtil.executePingRequest(IP,32,5000).getSuccessFlag())
                    future.completeExceptionally(new Throwable("PC returns pings"));
             else
                    future.complete(null);
            return future;
        };

        Command<?> turnOff = ComplexCommandBuilder.builder()
                .addCommand(0,"turn off",powerCommandExecutor.getCommand(IP,powerOff).thenWait(20000))
                .addCommand(1, "ping", ping).build();

        return builder
                .setData("IP",IP)
                .setFactory(VLCPlayerFactory.class)
                .setStateRequestCommand(getInitWithPingCheck(statusRequestExecutor.getCommand(IP,init),IP))
                .setCheckCommand(regularCheckExecutor.getCommand(IP,check))
                .setSwitchOnCommand(turnOn)
                .setSwitchOffCommand(turnOff)
                .addCommand("ping",ping)
                .addCommand("restart",powerCommandExecutor.getCommand(IP,powerRestart))
                .addCommand("play",statusRequestExecutor.getCommand(IP,play))
                .addCommand("send WoL",wolExecutor.getCommand(IP,0,mac))
                .setCheckResendTime(60*1000*3);
    }
    public static ManagedDevice buildDevice(String IP, String mac, ManagedDevice.ManagedDeviceBuilder builder) throws IllegalArgumentException {
        return buildClass(IP,mac,builder).build();
    }

}
