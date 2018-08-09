package org.gmig.gecs;

import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.factories.DatatonFactory;
import org.junit.Test;

/**
 * Created by brix on 4/19/2018.
 */
public class DatatonFactoryTest {
    @Test
    public void test() throws Exception {
        ManagedDevice p1 = DatatonFactory.build("10.8.0.111", "00:20:98:02:d8:52");
       p1.switchOnCmd().exec();
        //p1.getCommand("status").exec();
        //p1.switchOffCmd().exec();
        //p1.manager.beginChecks();
       // p1.stateReq().exec();
        //p1.switchOnCmd().exec();
        Thread.sleep(100000);
        //p1.switchOnCmd();
    }
    @Test
    public void testJSON() throws Exception {
        DatatonFactory f = new DatatonFactory();

        StructureReader reader = new StructureReader();
        reader.factories.add(new DatatonFactory());
        ManagedDevice p1 = reader.loadDevicesFromJSON(
                "{\"devices\":"+
                "[{" +
                "\"name\": \"PR-11.2\","+
                "\"factory\": \"DatatonFactory\"," +
                "\"ip\": \"10.8.0.111\"," +
                "\"mac\": \"00:20:98:02:d8:52\"," +
                "\"description\": \"Small story:Tolstaya\"" +
                "}]}").stream().findFirst().get();
        p1.queue.setCommandTimeout(500000);


        //p1.switchOnCmd().exec();
        //p1.getCommand("status").exec();
        p1.switchOffCmd().exec();
        //p1.manager.beginChecks();
        // p1.stateReq().exec();
        //p1.switchOnCmd().exec();
        Thread.sleep(10000000);
        //p1.switchOnCmd();
    }

}