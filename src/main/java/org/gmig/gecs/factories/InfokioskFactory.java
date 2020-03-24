package org.gmig.gecs.factories;

import com.fasterxml.jackson.databind.JsonNode;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.executors.WOLCommandExecutor;
import org.apache.log4j.Logger;

import java.io.IOException;


/**
 * Created by brix isOn 3/16/2018.
 */
public class InfokioskFactory extends ManagedDeviceFactory {

    private static final Logger logger = Logger.getLogger(ProjectorFactory.class);
    protected static WOLCommandExecutor wolExecutor = new WOLCommandExecutor();


    @Override
    protected ManagedDevice buildType(JsonNode jsonNode, ManagedDevice.ManagedDeviceBuilder builder) throws IOException {
        return build(jsonNode.get("ip").asText(),jsonNode.get("mac").asText(),builder);
    }
    public static ManagedDevice build(String IP,String mac) throws IllegalArgumentException {
        return build(IP,mac, ManagedDevice.newBuilder().setName(IP));
    }

    public static ManagedDevice build(String IP, String mac, ManagedDevice.ManagedDeviceBuilder builder) throws IllegalArgumentException {
        return VLCPlayerFactory.constructVLCPlayer(IP,mac,builder).setSwitchOnCommand(wolExecutor.getCommand(IP,9,mac)).build();
    }

}
