package org.gmig.gecs;

import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.junit.Test;
import org.quartz.SchedulerException;

import java.io.IOException;
import java.text.ParseException;

/**
 * Created by brix on 4/26/2018.
 */

public class LoaderTest {

    PJLinkDeviceModel m;

    NioSocketAcceptor acc1;
    NioSocketAcceptor acc2;
    NioSocketAcceptor acc3;
    NioSocketAcceptor acc4;

    UnitDeviceModel modPC1;
    UnitDeviceModel modPC2;
    @Test
    public void testLoad() throws ParseException, SchedulerException, IOException, InterruptedException, ClassNotFoundException {
        m = new PJLinkDeviceModel();
        acc1=m.setUpServer("127.0.0.1");
        //ManagedDevice p1 = ProjectorFactory.buildDevice("127.0.0.1");
        acc2=m.setUpServer("127.0.0.2");
        acc3=m.setUpServer("127.0.0.3");
        acc4=m.setUpServer("127.0.0.4");

        // ManagedDevice p2 = ProjectorFactory.buildDevice("127.0.0.2");
        Thread.sleep(100);
        modPC1 = new UnitDeviceModel();
        modPC1.setUpServer("127.0.0.5",11211);
        modPC1.setUpDaemon("127.0.0.5");
        Thread.sleep(100);
        modPC2 = new UnitDeviceModel();
        modPC2.setUpServer("127.0.0.6",11211);
        modPC2.setUpDaemon("127.0.0.6");
        Thread.sleep(100);

        Loader l = new Loader();

        //l.load();
        Thread.sleep(200000000);

    }

}