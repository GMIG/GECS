package org.gmig.gecs.factories;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.log4j.Logger;
import org.gmig.gecs.command.Command;
import org.gmig.gecs.command.ComplexCommandBuilder;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.executors.TCPCommandExecutor;
import org.gmig.gecs.executors.WOLCommandExecutor;
import org.gmig.gecs.reaction.Reaction;
import org.gmig.gecs.reaction.ReactionCloseWithSuccess;
import org.gmig.gecs.reaction.ReactionWrite;

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Created by brix on 4/19/2018.
 */
@SuppressWarnings("UnnecessaryLocalVariable")
public class DatatonFactory extends AbstractManagedDeviceFactory {
    private static final Logger logger = Logger.getLogger(DatatonFactory.class);


    private static int datatonPort = 3039;
    private static final TCPCommandExecutor datatonExecutor = new TCPCommandExecutor(textFilter,datatonPort);
    private static final TCPCommandExecutor statusRequestExecutor = new TCPCommandExecutor(textFilter,datatonPort);
    private static final TCPCommandExecutor datatonChecksExecutor = new TCPCommandExecutor(textFilter,datatonPort);
    private static final TCPCommandExecutor firstCheckExecutor = new TCPCommandExecutor(textFilter,datatonPort);

    private static final WOLCommandExecutor wolExecutor = new WOLCommandExecutor();
    private static final String okString = "Ready \"5.5.2\" \"WATCHPOINT\" \"WATCHPAX\" true";
    static {
        datatonChecksExecutor.setReconnectTimeMillis(2000);
        datatonChecksExecutor.setMaxReconnectTries(2);

        statusRequestExecutor.setReconnectTimeMillis(1000);
        statusRequestExecutor.setMaxReconnectTries(2);

        firstCheckExecutor.setReconnectTimeMillis(30000);
        firstCheckExecutor.setMaxReconnectTries(4);

        wolExecutor.setIpResendTimeMillis(15000);
        wolExecutor.setIpResendTries(5);
    }

    @Override
    protected ManagedDevice buildType(JsonNode node, ManagedDevice.ManagedDeviceBuilder builder) throws IOException{
        return build(node.get("ip").asText(), node.get("mac").asText(),builder);
    }

    public static ManagedDevice build(String IP, String mac) throws IllegalArgumentException,IOException {
        return build(IP,mac,ManagedDevice.newBuilder().setName(IP));
    }

     private static ManagedDevice build(String IP, String mac, ManagedDevice.ManagedDeviceBuilder builder) throws IllegalArgumentException,IOException {

        HashMap<Object,Reaction> init = Reaction.onConnectionTry(
                new ReactionWrite("ping")
                        .on(okString,isOn),
                isOff);

         HashMap<Object,Reaction> check = Reaction.onConnectionSuccess(
                 new ReactionWrite("ping")
                         .on(okString,new ReactionCloseWithSuccess()));

         Command<?> turnOn = ComplexCommandBuilder.builder()
                 .addCommand(0,"wol",wolExecutor.getCommand(IP,0,mac).thenWait(5000))
                 .addCommand(0, "check", firstCheckExecutor.getCommand(IP,check))
                 .collect(0);

        HashMap<Object,Reaction> powerOff = Reaction.onConnectionTry(
                 new ReactionWrite("authenticate 1")
                        .on(okString, new ReactionWrite("powerDown")
                                .on("class java.io.IOException",new ReactionCloseWithSuccess()))
                , new ReactionCloseWithSuccess());

        //Reply "WO2Launch" false 0 true true false 122 true
        String statusReq = "\nReply (\".*\") (false|true) (.) (false|true) (false|true) (false|true)";
        HashMap<Object,Reaction> status = Reaction.onConnectionSuccess(
                new ReactionWrite("authenticate 1")
                        .on(okString, new ReactionWrite("getStatus")
                                .on(Pattern.compile(statusReq),new ReactionCloseWithSuccess())));

        ManagedDevice unit = builder
                .setData("IP",IP)
                .setFactory(DatatonFactory.class)
                .setStateRequestCommand(statusRequestExecutor.getCommand(IP,init))
                .setCheckCommand(datatonChecksExecutor.getCommand(IP,check))
                .setSwitchOnCommand(turnOn)
                .setSwitchOffCommand(datatonExecutor.getCommand(IP,powerOff).thenWait(35000))
                .addCommand("get status",datatonExecutor.getCommand(IP,status))
                .setCheckResendTimeMinutes(3)
                .build();
        return unit;
    }

    public void dispose(){
        datatonExecutor.dispose();
        statusRequestExecutor.dispose();
        datatonChecksExecutor.dispose();
        firstCheckExecutor.dispose();
    }

}
