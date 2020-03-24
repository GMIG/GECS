package org.gmig.gecs.factories;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.log4j.Logger;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.executors.TCPCommandExecutor;
import org.gmig.gecs.reaction.Reaction;
import org.gmig.gecs.reaction.ReactionCloseWithSuccess;
import org.gmig.gecs.reaction.ReactionWrite;
import org.gmig.gecs.reaction.ReactionWriteArgument;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by brix on 4/19/2018.
 */
public class SharpTVFactory extends AbstractManagedDeviceFactory {
    private static final Logger logger = Logger.getLogger(SharpTVFactory.class);

    private static int sharpTVPort = 10002;
    private static TCPCommandExecutor sharpPowerExecutor = new TCPCommandExecutor(textFilter, sharpTVPort);
    private static TCPCommandExecutor sharpCheckExecutor = new TCPCommandExecutor(textFilter, sharpTVPort);

    @Override
    protected ManagedDevice buildType(JsonNode jsonNode, ManagedDevice.ManagedDeviceBuilder builder) throws IOException {
        return buildSharpTV(jsonNode.get("ip").asText(),builder);
    }

    @Override
    public void dispose() {

    }

    public static ManagedDevice build(String IP) throws IllegalArgumentException {
        return buildSharpTV(IP,ManagedDevice.newBuilder().setName(IP));
    }

    public static ManagedDevice buildSharpTV(String IP,ManagedDevice.ManagedDeviceBuilder builder) throws IllegalArgumentException {

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
                .addArgCommand("volume",sharpCheckExecutor.getArgCommand(IP,setVolume))
                .addCommand("turn on RSPW2",sharpCheckExecutor.getCommand(IP,turnOnPowrOn).thenWait(3000))
                .setCheckResendTimeMinutes(2)
                .build();
        //unit.getArgCommand("vol").exec(10);
        return unit;
    }
}
