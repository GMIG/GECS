package org.gmig.gecs.factories;

import com.fasterxml.jackson.databind.JsonNode;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.executors.TCPCommandExecutor;
import org.apache.log4j.Logger;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;
import org.apache.mina.filter.codec.textline.TextLineEncoder;
import org.gmig.gecs.reaction.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;

public class ProjectorFactory extends ManagedDeviceFactory {
    private static final Logger logger = Logger.getLogger(ProjectorFactory.class);

    private static ProtocolCodecFilter textFilter = new ProtocolCodecFilter(
            new TextLineEncoder(Charset.forName("UTF-8"), LineDelimiter.MAC),new TextLineDecoder(Charset.forName("UTF-8"), LineDelimiter.MAC));
    private static TCPCommandExecutor statusRequestExecutor = new TCPCommandExecutor(textFilter,4352);
    private static TCPCommandExecutor checkExecutor = new TCPCommandExecutor(textFilter,4352);

    private static TCPCommandExecutor powerCommandExecutor = new TCPCommandExecutor(textFilter,4352);

    static {
        powerCommandExecutor.setReadTimeoutSeconds(10000);

        statusRequestExecutor.setReconnectTimeMillis(1000);
        statusRequestExecutor.setMaxReconnectTries(2);
        statusRequestExecutor.setReadTimeoutSeconds(10000);

        checkExecutor.setReconnectTimeMillis(2000);
        checkExecutor.setMaxReconnectTries(2);
        checkExecutor.setReadTimeoutSeconds(10000);
    }

    @Override
    protected ManagedDevice buildType(JsonNode jsonNode, ManagedDevice.ManagedDeviceBuilder builder) throws IOException {
        return build(jsonNode.get("ip").asText(),builder);
    }

    public static ManagedDevice build(String IP) throws IllegalArgumentException {
        return build(IP,ManagedDevice.newBuilder().setName(IP));
    }

    public static ManagedDevice build(String IP, ManagedDevice.ManagedDeviceBuilder builder)  throws IllegalArgumentException{
        HashMap<Object,Reaction> error = new HashMap<>();
        error.put("%1POWR=ERR3",new ReactionCloseWithError("PJLink Error: Unavailable time"));
        error.put("%1POWR=ERR4",new ReactionCloseWithError("PJLink Error: Projector/Display failure"));

        HashMap<Object,Reaction> wrapUp = new HashMap<>();
        wrapUp.put("%1POWR=OK",new ReactionCloseWithSuccess());
        wrapUp.putAll(error);
        HashMap<Object,Reaction> init = Reaction.onConnectionSuccess(
                new ReactionDoNothing().on("PJLINK 0",new ReactionWrite("%1POWR ?")
                        .on("%1POWR=1",isOn)
                        .on("%1POWR=0",isOff)
                        .addMap(error)));
        HashMap<Object,Reaction> check = Reaction.onConnectionSuccess(
                new ReactionDoNothing().on("PJLINK 0",new ReactionWrite("%1POWR ?")
                        .on("%1POWR=1",new ReactionCloseWithSuccess())
                        .addMap(error)));
        HashMap<Object,Reaction> powerOn = Reaction.onConnectionSuccess(
                (new ReactionDoNothing().on("PJLINK 0", new ReactionWrite("%1POWR 1")
                        .addMap(wrapUp))));
        HashMap<Object,Reaction> powerOff = Reaction.onConnectionSuccess(
                (new ReactionDoNothing().on("PJLINK 0", new ReactionWrite("%1POWR 0")
                        .addMap(wrapUp))));
        ManagedDevice d = builder
                .setData("IP",IP)
                .setFactory(ProjectorFactory.class)
                .setSwitchOffCommand(powerCommandExecutor.getCommand(IP,powerOff))
                .setSwitchOnCommand(powerCommandExecutor.getCommand(IP,powerOn))
                .setCheckCommand(checkExecutor.getCommand(IP,check))
                .setStateRequestCommand(statusRequestExecutor.getCommand(IP,init))
                .setCheckResendTime(60*1000*60)
                .build();
        return d;
    }
}
