package org.gmig.gecs.factories;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;
import org.apache.mina.filter.codec.textline.TextLineEncoder;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.device.StateRequestResult;
import org.gmig.gecs.reaction.ReactionCloseWithSuccess;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by brix on 4/20/2018.
 */
public abstract class AbstractManagedDeviceFactory {

     protected static ProtocolCodecFilter textFilter = new ProtocolCodecFilter(
             new TextLineEncoder(Charset.forName("UTF-8"), LineDelimiter.MAC),
             new TextLineDecoder(Charset.forName("UTF-8"), LineDelimiter.MAC));

     public ManagedDevice fromJSON(JsonNode node) throws IOException{
          ManagedDevice.ManagedDeviceBuilder builder = ManagedDevice.newBuilder();
          builder.setName(node.get("name").asText());
          builder.setDescription(node.get("description").asText());
          return buildType(node, builder);
     }

     abstract protected ManagedDevice buildType(JsonNode node,ManagedDevice.ManagedDeviceBuilder builder) throws IOException;

     protected static final ReactionCloseWithSuccess isOn = new ReactionCloseWithSuccess().setResultProcessor(StateRequestResult::IsOn);
     protected static final ReactionCloseWithSuccess isOff = new ReactionCloseWithSuccess().setResultProcessor(StateRequestResult::IsOff);

     abstract public void dispose();
}
