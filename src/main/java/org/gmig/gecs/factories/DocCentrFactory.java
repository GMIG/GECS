package org.gmig.gecs.factories;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.log4j.Logger;
import org.gmig.gecs.command.Command;
import org.gmig.gecs.command.ComplexCommandBuilder;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.device.StateRequestResult;
import org.gmig.gecs.executors.TCPCommandExecutor;
import org.gmig.gecs.reaction.Reaction;
import org.gmig.gecs.reaction.ReactionCloseWithSuccess;
import org.gmig.gecs.reaction.ReactionWrite;
import org.icmp4j.IcmpPingUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 *
 */
public class DocCentrFactory extends PCWithDaemonFactory{
    private static final Logger logger = Logger.getLogger(DocCentrFactory.class);

    private static final TCPCommandExecutor relayCommandExecutor = new TCPCommandExecutor(textFilter, 11201);

    @Override
    protected ManagedDevice buildType(JsonNode jsonNode, ManagedDevice.ManagedDeviceBuilder builder) throws IOException {
        return buildDocCentr(jsonNode.get("ipRelay").asText(),jsonNode.get("ipStick1").asText(),jsonNode.get("ipStick2").asText(),builder);
    }

    @Override
    public void dispose(){
        super.dispose();
        relayCommandExecutor.dispose();
    }

    public static ManagedDevice build(String IPRelay,String IPStick1,String IPStick2,String IPStick3) throws IllegalArgumentException {
        return buildDocCentr(IPRelay,IPStick1, IPStick2, ManagedDevice.newBuilder().setName(IPRelay));
    }

    private static boolean pings(String IP){
        return IcmpPingUtil.executePingRequest(IP,32,5000).getSuccessFlag();
    }

    private static Command<Void> getPingCommand(String IP){
        return ()->{
            CompletableFuture<Void> future = new CompletableFuture<>();
            if (pings(IP))
                future.complete(null);
            else
                future.completeExceptionally(new Throwable(IP + " does not return ping"));
            return future;
        };
    }

    private static Command<StateRequestResult> getInitCommand(String IP){
        return ()->{
            CompletableFuture<StateRequestResult> future = new CompletableFuture<>();
            if (pings(IP))
                future.complete(StateRequestResult.IsOn(null));
            else
                future.complete(StateRequestResult.IsOff(null));
            return future;
        };
    }


    public static ManagedDevice buildDocCentr(String IPRelay,String IPStick1,String IPStick2,ManagedDevice.ManagedDeviceBuilder builder) throws IllegalArgumentException {

        Command<HashMap<String,?>> check = ComplexCommandBuilder.builder()
                .addCommand(0,"pingRelay",getPingCommand(IPRelay))
                .addCommand(0,"pingStick1",getPingCommand(IPStick1))
                .addCommand(0,"pingStick2",getPingCommand(IPStick2))
                .collect(0);
        Command<StateRequestResult> init= ()->{
            CompletableFuture<StateRequestResult> future = new CompletableFuture<>();
            CompletableFuture<HashMap<String,?>> getInit = ComplexCommandBuilder.builder()
                    .addCommand(0,"pingRelay",getInitCommand(IPRelay))
                    .addCommand(0,"pingStick1",getInitCommand(IPStick1))
                    .addCommand(0,"pingStick2",getInitCommand(IPStick2))
                    .collect(0)
                    .get();
            getInit.handle((r,t)->{
                if(t != null){
                    future.completeExceptionally(t);
                    return null;
                }
                HashMap<String,?> s = new HashMap<> (r);
                s.remove("pingRelay");
                Object first = s.values().iterator().next();
                boolean allEqual = true;
                String str = "";
                for (Map.Entry<String,?> e : s.entrySet()) {
                    str += " " + e.getKey() + ":" + e.getValue();
                    if(!e.getValue().equals(first) )
                        allEqual = false;
                }
                if(allEqual)
                    return future.complete((StateRequestResult) first);
                else {
                    future.completeExceptionally(new Throwable(str));
                }
                return null;
            });
            return future;
        };
        HashMap<Object, Reaction> powerOnRelay = Reaction.onConnectionSuccess(
                new ReactionWrite("switch")
                        .on("OK", new ReactionCloseWithSuccess()));
        Command<?> switchOn = relayCommandExecutor.getCommand(IPRelay,powerOnRelay).thenWait(60*2*1000);
        Command<HashMap<String,?>>  switchOff = ComplexCommandBuilder.builder()
                .addCommand(0,"switchOffStick1", daemonCommandExecutor.getCommand(IPStick1, daemonSwitchOff))
                .addCommand(0,"switchOffStick2", daemonCommandExecutor.getCommand(IPStick2, daemonSwitchOff))
                .collect(0);
        ManagedDevice unit = builder
                .setData("IP",IPRelay)
                .setFactory(DocCentrFactory.class)
                .setStateRequestCommand(init)
                .setCheckCommand(check)
                .setSwitchOnCommand(switchOn)
                .setSwitchOffCommand(switchOff)
                .setCheckResendTimeMinutes(3)
                .build();
        //unit.getArgCommand("vol").exec(10);
        return unit;
    }

}
