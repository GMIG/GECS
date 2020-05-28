package org.gmig.gecs.factories;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.log4j.Logger;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.executors.TCPCommandExecutor;
import org.gmig.gecs.reaction.*;

import java.io.IOException;
import java.util.HashMap;

public class RDaemonFactory extends AbstractManagedDeviceFactory {
    private static final Logger logger = Logger.getLogger(RDaemonFactory.class);

    private static int rDaemonPort = 8007;
    private static TCPCommandExecutor statusRequestExecutor = new TCPCommandExecutor(textFilter,rDaemonPort);
    private static TCPCommandExecutor checkExecutor = new TCPCommandExecutor(textFilter,rDaemonPort);
    private static TCPCommandExecutor powerCommandExecutor = new TCPCommandExecutor(textFilter,rDaemonPort);

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
        HashMap<Object,Reaction> init = Reaction.onConnectionSuccess(
                new  ReactionWrite("dem.check()")
                        .on("dem:is_on",isOn)
                        .on("dem:is_off",isOff));
        HashMap<Object,Reaction> check = Reaction.onConnectionSuccess(
                new  ReactionWrite("dem.check()")
                        .on("dem:is_on",new ReactionCloseWithSuccess()));
        HashMap<Object,Reaction> powerOn = Reaction.onConnectionSuccess(
                new  ReactionWrite("dem.switch_on()")
                        .on("dem:ok",new ReactionCloseWithSuccess())
                        .on("dem:err",new ReactionCloseWithError("RDaemon was not able to execute program")));
        HashMap<Object,Reaction> powerOff = Reaction.onConnectionSuccess(
                new  ReactionWrite("dem.switch_off()")
                        .on("dem:ok",new ReactionCloseWithSuccess())
                        .on("dem:err",new ReactionCloseWithError("RDaemon was not able to close program")));
        HashMap<Object,Reaction> restart = Reaction.onConnectionSuccess(
                new  ReactionWrite("dem.restart()")
                        .on("dem:ok",new ReactionCloseWithSuccess())
                        .on("dem:err",new ReactionCloseWithError("RDaemon was not able to close program")));

        ManagedDevice d = builder
                .setData("IP",IP)
                .setFactory(RDaemonFactory.class)
                .setSwitchOffCommand(powerCommandExecutor.getCommand(IP,powerOff).thenWait(2000))
                .setSwitchOnCommand(powerCommandExecutor.getCommand(IP,powerOn).thenWait(2000))
                .setCheckCommand(checkExecutor.getCommand(IP,check))
                .setStateRequestCommand(statusRequestExecutor.getCommand(IP,init))
                .addCommand("restart", powerCommandExecutor.getCommand(IP,restart))
                .setCheckResendTimeMinutes(6)
                .build();
        return d;
    }
}
