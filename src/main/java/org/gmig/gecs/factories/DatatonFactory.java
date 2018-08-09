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
import org.apache.log4j.Logger;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;
import org.apache.mina.filter.codec.textline.TextLineEncoder;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * Created by brix on 4/19/2018.
 */
public class DatatonFactory extends ManagedDeviceFactory {
    private static final Logger logger = Logger.getLogger(DatatonFactory.class);

    private static ProtocolCodecFilter textFilter = new ProtocolCodecFilter(
            new TextLineEncoder(Charset.forName("UTF-8"), LineDelimiter.MAC),new TextLineDecoder(Charset.forName("UTF-8"), LineDelimiter.MAC));

    private static TCPCommandExecutor datatonExecutor = new TCPCommandExecutor(textFilter,3039);
    private static TCPCommandExecutor statusRequestExecutor = new TCPCommandExecutor(textFilter,3039);
    private static TCPCommandExecutor datatonChecksExecutor = new TCPCommandExecutor(textFilter,3039);
    private static TCPCommandExecutor firstCheckExecutor = new TCPCommandExecutor(textFilter,3039);

    private static WOLCommandExecutor wolExecutor = new WOLCommandExecutor();
    private static final String okString = "Ready \"5.5.2\" \"WATCHPOINT\" \"WATCHPAX\" true";
    static {
        datatonChecksExecutor.setReconnectTimeMillis(2000);
        datatonChecksExecutor.setMaxReconnectTries(2);

        statusRequestExecutor.setReconnectTimeMillis(1000);
        statusRequestExecutor.setMaxReconnectTries(2);

        firstCheckExecutor.setReconnectTimeMillis(20000);
        firstCheckExecutor.setMaxReconnectTries(2);

        wolExecutor.setIpResendTimeMillis(10000);
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
                 .addCommand(0,"WoL",wolExecutor.getCommand(IP,0,mac).thenWait(5000))
                 .addCommand(0, "check", firstCheckExecutor.getCommand(IP,check)).collect(0);

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
                .setSwitchOffCommand(datatonExecutor.getCommand(IP,powerOff))
                .addCommand("status",datatonExecutor.getCommand(IP,status))
                .setCheckResendTime(60*1000*10)
                .build();
        return unit;
    }

}
