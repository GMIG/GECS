package org.gmig.gecs.factories;

import com.fasterxml.jackson.databind.JsonNode;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.executors.TCPCommandExecutor;
import org.gmig.gecs.reaction.Reaction;
import org.gmig.gecs.reaction.ReactionCloseWithSuccess;
import org.gmig.gecs.reaction.ReactionWrite;
import org.gmig.gecs.reaction.ReactionWriteArgument;
import org.apache.log4j.Logger;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;
import org.apache.mina.filter.codec.textline.TextLineEncoder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Created by brix on 4/19/2018.
 */
public class SharpTVFactory extends ManagedDeviceFactory {
    private static final Logger logger = Logger.getLogger(DatatonFactory.class);

    private static ProtocolCodecFilter textFilter = new ProtocolCodecFilter(
            new TextLineEncoder(Charset.forName("UTF-8"), LineDelimiter.MAC), new TextLineDecoder(Charset.forName("UTF-8"), LineDelimiter.MAC));
    private static TCPCommandExecutor sharpPowerExecutor = new TCPCommandExecutor(textFilter, 10002);
    private static TCPCommandExecutor sharpCheckExecutor = new TCPCommandExecutor(textFilter, 10002);

    @Override
    protected ManagedDevice buildType(JsonNode jsonNode, ManagedDevice.ManagedDeviceBuilder builder) throws IOException {
        return build(jsonNode.get("ip").asText(),builder);
    }

    public static ManagedDevice build(String IP) throws IllegalArgumentException {
        return build(IP,ManagedDevice.newBuilder().setName(IP));
    }

    public static ManagedDevice build(String IP,ManagedDevice.ManagedDeviceBuilder builder) throws IllegalArgumentException {

        HashMap<Object, Reaction> init = Reaction.onConnectionSuccess(
                new ReactionWrite("POWR????")
                        .on("1", isOn)
                        .on("0", isOff));
        HashMap<Object, Reaction> check = Reaction.onConnectionSuccess(
                new ReactionWrite("POWR????")
                        .on("1", new ReactionCloseWithSuccess()));

        HashMap<Object, Reaction> powerOff = Reaction.onConnectionTry(
                new ReactionWrite("POWR0   ")
                        .on("OK", new ReactionCloseWithSuccess())
                , new ReactionCloseWithSuccess());
        HashMap<Object, Reaction> powerOn = Reaction.onConnectionSuccess(
                new ReactionWrite("POWR1   ")
                        .on("OK", new ReactionCloseWithSuccess()));
        HashMap<Object, Reaction> setVolume = Reaction.onConnectionSuccess(
                new ReactionWriteArgument((o)-> {
                    int arg = (int) o;
                    if (arg<10)
                        return "VOLM" + arg + "   ";
                    if (arg>99)
                        return "VOLM" + arg + " ";
                    else
                        return "VOLM" + arg + "  ";
                    }).on("OK", new ReactionCloseWithSuccess()));
        HashMap<Object, Reaction> turnOnPowrOn = Reaction.onConnectionSuccess(
                new ReactionWrite("RSPW2   ")
                        .on("OK", new ReactionCloseWithSuccess()));


        ManagedDevice unit = builder
                .setData("IP",IP)
                .setFactory(SharpTVFactory.class)
                .setStateRequestCommand(sharpCheckExecutor.getCommand(IP, init))
                .setCheckCommand(sharpCheckExecutor.getCommand(IP, check))
                .setSwitchOnCommand(sharpPowerExecutor.getCommand(IP,powerOn).thenWait(40000))
                .setSwitchOffCommand(sharpPowerExecutor.getCommand(IP, powerOff).thenWait(3000))
                .addArgCommand("vol",sharpCheckExecutor.getArgCommand(IP,setVolume))
                .addCommand("Turn on POWR 1",sharpCheckExecutor.getCommand(IP,turnOnPowrOn).thenWait(3000))
                .build();
        //unit.getArgCommand("vol").exec(10);
        unit.manager.setDelayMillis(100000);
        return unit;
    }
}
