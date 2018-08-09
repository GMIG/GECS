package org.gmig.gecs;

import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.factories.SharpTVFactory;
import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * Created by brix on 4/20/2018.
 */
public class SharpTVFactoryTest {

    private static final Logger logger = Logger.getLogger(SharpTVFactoryTest.class);


    @Test
    public void testTV() throws Exception{
        ManagedDevice tv = SharpTVFactory.build("10.8.0.125");
       // ManagedDevice tv = SharpTVFactory.buildDevice("127.0.0.1");
        //tv.getArgCommand("vol").exec(10);
        //tv.getArgCommand("vol").exec(20);
        //tv.getArgCommand("vol").exec(0);
        tv.switchOnCmd().exec();
       // tv.switchOffCmd().exec();
        //tv.switchOnCmd().exec();
        tv.stateReq().exec();


        Thread.sleep(20000);
    }

}