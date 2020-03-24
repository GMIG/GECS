package org.gmig.gecs.factories;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.log4j.Logger;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.executors.TCPCommandExecutor;
import org.gmig.gecs.reaction.*;

import java.io.IOException;
import java.util.HashMap;

public class ProjectorFactory extends AbstractManagedDeviceFactory {
    private static final Logger logger = Logger.getLogger(ProjectorFactory.class);

    private static int pjLinkPort = 4352;
    private static TCPCommandExecutor statusRequestExecutor = new TCPCommandExecutor(textFilter,pjLinkPort);
    private static TCPCommandExecutor checkExecutor = new TCPCommandExecutor(textFilter,pjLinkPort);
    private static TCPCommandExecutor powerCommandExecutor = new TCPCommandExecutor(textFilter,pjLinkPort);

    static {
        powerCommandExecutor.setReadTimeoutSeconds(10000);
        powerCommandExecutor.setMaxReconnectTries(4);

        statusRequestExecutor.setReconnectTimeMillis(4000);
        statusRequestExecutor.setMaxReconnectTries(3);
        statusRequestExecutor.setReadTimeoutSeconds(10000);

        checkExecutor.setReconnectTimeMillis(2000);
        checkExecutor.setMaxReconnectTries(4);
        checkExecutor.setReadTimeoutSeconds(10000);
    }

    @Override
    protected ManagedDevice buildType(JsonNode jsonNode, ManagedDevice.ManagedDeviceBuilder builder) throws IOException {
        return buildProjector(jsonNode.get("ip").asText(),builder);
    }

    @Override
    public void dispose() {
        statusRequestExecutor.dispose();
        checkExecutor.dispose();
        powerCommandExecutor.dispose();
    }

    public static ManagedDevice build(String IP) throws IllegalArgumentException {
        return buildProjector(IP,ManagedDevice.newBuilder().setName(IP));
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static ManagedDevice buildProjector(String IP, ManagedDevice.ManagedDeviceBuilder builder)  throws IllegalArgumentException{
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
                        .on("%1POWR=OK",new ReactionCloseWithSuccess())
                        .on("%1POWR=ERR3",new ReactionCloseWithSuccess().setResultProcessor((o)->"Projector already off (err3 received)"))
                        .on("%1POWR=ERR4",new ReactionCloseWithError("PJLink Error: Projector/Display failure"))
                    )
                )
            );
        ManagedDevice d = builder
                .setData("IP",IP)
                .setFactory(ProjectorFactory.class)
                .setSwitchOffCommand(powerCommandExecutor.getCommand(IP,powerOff))
                .setSwitchOnCommand(powerCommandExecutor.getCommand(IP,powerOn))
                .setCheckCommand(checkExecutor.getCommand(IP,check))
                .setStateRequestCommand(statusRequestExecutor.getCommand(IP,init))
                .setCheckResendTimeMinutes(5)
                .build();
        return d;
    }
}
