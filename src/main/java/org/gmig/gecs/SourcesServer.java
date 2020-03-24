package org.gmig.gecs;

import com.fasterxml.jackson.databind.JsonNode;
import org.gmig.gecs.device.Commandable;
import org.gmig.gecs.device.Device;
import org.gmig.gecs.executors.TCPCommandAcceptor;
import org.gmig.gecs.reaction.ReactionCloseWithSuccess;
import org.apache.log4j.Logger;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;
import org.apache.mina.filter.codec.textline.TextLineEncoder;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Created by brix on 8/2/2018.
 */
public class SourcesServer {

    public Set<Device> getSources() {
        return Collections.unmodifiableSet(sources);
    }
    public Map<Commandable,Device> getCommandables() {
        return Collections.unmodifiableMap(inDevices);
    }

    private HashSet<Device> sources = new HashSet<>();
    private HashMap<Commandable,Device> inDevices = new HashMap<>();

    protected static final Logger logger = Logger.getLogger(SourcesServer.class);

    protected static ProtocolCodecFilter textFilter = new ProtocolCodecFilter(
            new TextLineEncoder(Charset.forName("UTF-8"), LineDelimiter.MAC),new TextLineDecoder(Charset.forName("UTF-8"), LineDelimiter.MAC));

    private TCPCommandAcceptor acceptor = new TCPCommandAcceptor(textFilter,11213);

    public Device addSource(JsonNode sourceJSON, HashSet<? extends Commandable> allDevices) throws ClassNotFoundException {
        String name = sourceJSON.get("name").asText();

        Device.DeviceBuilder builder = Device.builder()
                .setName(name)
                .setFactory(SourcesServer.class);
        for(JsonNode signal :sourceJSON.get("signals")) {
            String signalName = signal.get("name").asText();
            builder.addCommand(signalName, () -> CompletableFuture.completedFuture(signalName));
        }
        Device source = builder.build();

        sources.add(source);

        for(JsonNode signal :sourceJSON.get("signals")) {
            String signalName = signal.get("name").asText();
            for (final JsonNode action : signal.get("actions")) {
                String deviceName = action.get("device").asText();
                String command = action.get("command").asText();
                Optional<? extends Commandable> deviceO = allDevices.stream().filter((dev) -> dev.getName().equals(deviceName)).findFirst();
                if (deviceO.isPresent()) {
                    Commandable device = deviceO.get();
                    inDevices.put(device, source);
                    source.getCommand(signalName).success.add((o) -> device.getCommand(command).exec());
                } else
                    throw new ClassNotFoundException("Device " + deviceName + " not found for Source " + name + " and signal " + signal);
            }

            source.getArgCommandList()
                    .forEach((cmdName, command) ->
                            acceptor.addRule(command.getName(), new ReactionCloseWithSuccess()
                                                                        .afterCompletionAccept((o) -> command.exec(o))));
        }
        return source;
    }
    public void dispose(){
        acceptor.dispose();
    }

}
