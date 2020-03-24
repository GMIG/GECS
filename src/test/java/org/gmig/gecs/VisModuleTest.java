package org.gmig.gecs;

import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.factories.ProjectorFactory;
import org.gmig.gecs.factories.VLCPlayerFactory;
import org.gmig.gecs.groups.VisModule;
import org.junit.After;
import org.junit.Test;

/**
 * Created by brix on 1/15/2019.
 */
public class VisModuleTest {
    private PJLinkDeviceModel m;

    private NioSocketAcceptor acc;
    private NioSocketAcceptor acc1;
    private NioSocketAcceptor acc2;
    private UnitDeviceModel modPC;
    private UnitDeviceModel modPC1;

    @Test
    public void testTwo() throws Exception{
        m = new PJLinkDeviceModel();
        acc=m.setUpServer("127.0.0.1");
        ManagedDevice p1 = ProjectorFactory.build("127.0.0.1");
        acc1=m.setUpServer("127.0.0.2");
        ManagedDevice p2 = ProjectorFactory.build("127.0.0.2");
        Thread.sleep(100);
        modPC = new UnitDeviceModel();
        acc1 = modPC.setUpServer("127.0.0.3",11211);
        acc2 = modPC.setUpDaemon("127.0.0.3");
        Thread.sleep(100);
        ManagedDevice pc = VLCPlayerFactory.buildDevice("127.0.0.3","11:11:11:11:11:11");

        VisModule f = VisModule.newBuilder()
                .setVideoSource(pc)
                .addVisualiser(p1)
                .addVisualiser(p2)
                .build();
        //pc.stateReq().exec();
        f.switchOnCmd().exec();
        Thread.sleep(10000);
        //f.switchOffCmd().exec();
        Thread.sleep(30000);

    }
    @After
    public void tearDown() throws Exception {
        if (acc!=null)
            acc.dispose();
        if (acc1!=null)
            acc1.dispose();
        if (acc2!=null)
            acc2.dispose();
        if(m!=null)
            m.tearDown();

    }


}