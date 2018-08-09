package org.gmig.gecs;

import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.device.StateRequestResult;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.concurrent.CompletableFuture;

/**
 * Created by brix isOn 2/26/2018.
 */
public class DeviceTest  {
    private static final Logger logger = Logger.getLogger(DeviceTest.class);


    Object a ;
    @Test
    public void testChecks() throws Exception{
    a= null;

    ManagedDevice test = ManagedDevice.newBuilder()
            .setName("test")
            .setCheckCommand(()->{logger.info("check");return CompletableFuture.completedFuture(null);})
            .setStateRequestCommand(()->{logger.info("state request");return CompletableFuture.completedFuture(StateRequestResult.IsOn(null));})
            .setSwitchOffCommand(()->{logger.info("switch off");return CompletableFuture.completedFuture(null);})
            .setSwitchOnCommand(()->{logger.info("switch off");return CompletableFuture.completedFuture(null);})
            .build();
    //test.manager.setDelayMillis(500);
    test.stateReq().exec();
    Thread.sleep(200000);




    }


}