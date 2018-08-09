package org.gmig.gecs.factories;

import com.fasterxml.jackson.databind.JsonNode;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.executors.TCPCommandExecutor;
import org.gmig.gecs.executors.WOLCommandExecutor;
import org.gmig.gecs.reaction.Reaction;
import org.gmig.gecs.reaction.ReactionCloseWithError;
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

/**
 * Created by brix isOn 3/16/2018.
 */
public class VLCTestPlayerFactory extends ManagedDeviceFactory {

    private static final Logger logger = Logger.getLogger(ProjectorFactory.class);

    private static ProtocolCodecFilter textFilter = new ProtocolCodecFilter(
            new TextLineEncoder(Charset.forName("UTF-8"), LineDelimiter.MAC),new TextLineDecoder(Charset.forName("UTF-8"), LineDelimiter.MAC));
    //private static TCPCommandExecutor checkExecutor = new TCPCommandExecutor(textFilter,11211);
    private static TCPCommandExecutor checkExecutor = new TCPCommandExecutor(textFilter,10203);
    private static TCPCommandExecutor initRequestExecutor = new TCPCommandExecutor(textFilter,10203);

    private static TCPCommandExecutor powerCommandExecutor = new TCPCommandExecutor(textFilter,10203);
    protected static WOLCommandExecutor wolExecutor = new WOLCommandExecutor();

    static {
        checkExecutor.setReconnectTimeMillis(5000);
        initRequestExecutor.setMaxReconnectTries(1000);
        initRequestExecutor.setMaxReconnectTries(3);
        wolExecutor.setIpResendTimeMillis(10000);
    }


    @Override
    protected ManagedDevice buildType(JsonNode jsonNode, ManagedDevice.ManagedDeviceBuilder builder) throws IOException {
        return build(jsonNode.get("ip").asText(),jsonNode.get("mac").asText(),builder);
    }
    public static ManagedDevice build(String IP,String mac) throws IllegalArgumentException {
        return build(IP,mac, ManagedDevice.newBuilder().setName(IP));
    }

    public static ManagedDevice.ManagedDeviceBuilder buildClass(String IP, String mac, ManagedDevice.ManagedDeviceBuilder builder) throws IllegalArgumentException {

        HashMap<Object, Reaction> error = new HashMap<>();
        error.put("1", new ReactionCloseWithError("No Sound"));
        error.put("2", new ReactionCloseWithError("No Video"));
        error.put("5", new ReactionCloseWithError("External COM device not found"));

        HashMap<Object,Reaction> init = Reaction.onConnectionTry(
                new ReactionWrite("STATUS").on("0",isOn)
                        .on("1",isOn)
                        .on("2",new ReactionWrite("ERROR").addMap(error))
                ,
                isOff);
        HashMap<Object,Reaction> check = Reaction.onConnectionSuccess(
                new ReactionWrite("STATUS")
                        .on("0",new ReactionCloseWithSuccess())
                        .on("1",new ReactionCloseWithSuccess())
                        .on("2",new ReactionWrite("ERROR").addMap(error)));

        HashMap<Object,Reaction> powerOff = Reaction.onConnectionTry(
                new ReactionWrite("SHUTDOWN")
                        .on("class java.io.IOException",new ReactionCloseWithSuccess()),
                new ReactionCloseWithSuccess());
        HashMap<Object,Reaction> powerRestart = Reaction.onConnectionSuccess(
                (new ReactionWrite("RESTART").thenAwaitClosing(new ReactionCloseWithSuccess())));

        return builder
                .setData("IP",IP)
                .setFactory(VLCTestPlayerFactory.class)
                .setStateRequestCommand(initRequestExecutor.getCommand(IP,init))
                .setCheckCommand(checkExecutor.getCommand(IP,check))
                .setSwitchOnCommand(wolExecutor.getCommand(IP,0,mac))
                .setSwitchOffCommand(powerCommandExecutor.getCommand(IP,powerOff))
                .addCommand("restart",powerCommandExecutor.getCommand(IP,powerRestart));
    }
    public static ManagedDevice build(String IP, String mac, ManagedDevice.ManagedDeviceBuilder builder) throws IllegalArgumentException {
        return buildClass(IP,mac,builder).build();
    }

}
