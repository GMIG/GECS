package org.gmig.gecs.factories;

import com.fasterxml.jackson.databind.JsonNode;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.device.StateRequestResult;
import org.gmig.gecs.reaction.ReactionCloseWithSuccess;

import java.io.IOException;

/**
 * Created by brix on 4/20/2018.
 */
public abstract class ManagedDeviceFactory {

     abstract protected ManagedDevice buildType(JsonNode node,ManagedDevice.ManagedDeviceBuilder builder) throws IOException;

     public ManagedDevice fromJSON(JsonNode node) throws IOException{
          ManagedDevice.ManagedDeviceBuilder builder = ManagedDevice.newBuilder();
          builder.setName(node.get("name").asText());
          builder.setDescription(node.get("description").asText());
          return buildType(node, builder);
     }
     protected static final ReactionCloseWithSuccess isOn = new ReactionCloseWithSuccess().setResultProcessor(StateRequestResult::IsOn);
     protected static final ReactionCloseWithSuccess isOff = new ReactionCloseWithSuccess().setResultProcessor(StateRequestResult::IsOff);


}
