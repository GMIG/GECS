package org.gmig.gecs;

import com.fasterxml.jackson.databind.JsonNode;
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
    public Map<Device,Device> getDevices() {
        return Collections.unmodifiableMap(inDevices);
    }

    private HashSet<Device> sources = new HashSet<>();
    private HashMap<Device,Device> inDevices = new HashMap<>();

    protected static final Logger logger = Logger.getLogger(SourcesServer.class);

    protected static ProtocolCodecFilter textFilter = new ProtocolCodecFilter(
            new TextLineEncoder(Charset.forName("UTF-8"), LineDelimiter.MAC),new TextLineDecoder(Charset.forName("UTF-8"), LineDelimiter.MAC));

    public TCPCommandAcceptor acceptor = new TCPCommandAcceptor(textFilter,11213);

    public Device addSource(JsonNode sourceJSON, HashSet<? extends Device> devices) {
        String name = sourceJSON.get("name").asText();
        String signal = sourceJSON.get("signal").asText();
        Device source = Device.builder()
                .setName(name)
                .setFactory(SourcesServer.class)
                .addCommand(signal, () -> CompletableFuture.completedFuture(signal)).build();
        for (final JsonNode action : sourceJSON.get("actions")) {
            String deviceName = action.get("device").asText();
            String command = action.get("command").asText();
            Optional<? extends Device> deviceO = devices.stream().filter((dev) -> dev.getName().equals(deviceName)).findFirst();
            if (deviceO.isPresent()) {
                Device device = deviceO.get();
                inDevices.put(device,source);
                source.getCommand(signal).success.add((o) -> device.getCommand(command).exec());
            } else
                throw new IllegalArgumentException("Device " + deviceName + " not found for Source " + name + " and signal " + signal);
        }

        source.getArgCommandList().forEach((cmdName,command)->{
            acceptor.addRule(command.getName(),new ReactionCloseWithSuccess()
                    .afterCompletionAccept((o)->command.exec(o)));
        });
        sources.add(source);
        return source;
    }

}
