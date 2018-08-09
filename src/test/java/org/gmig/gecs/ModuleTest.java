package org.gmig.gecs;

import org.gmig.gecs.device.ManagedDevice;
import org.gmig.gecs.device.Switchable;
import org.gmig.gecs.factories.ProjectorFactory;
import org.gmig.gecs.factories.VLCPlayer2ScreenFactory;
import org.gmig.gecs.factories.VLCPlayerFactory;
import org.gmig.gecs.groups.Module;
import org.gmig.gecs.groups.SwitchGroup;
import org.apache.log4j.Logger;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Created by brix on 4/17/2018.
 */
public class ModuleTest {
    private static final Logger logger = Logger.getLogger(ModuleTest.class);

    @Before
    public void setUp() throws Exception {
   }
   @Test
   public void testMultiple() throws Exception{
       PJLinkDeviceModel m = new PJLinkDeviceModel();
       m.isError = true;
       m.setUpServer("127.0.0.1").getHandler();
       PJLinkDeviceModel m2 = new PJLinkDeviceModel();
       m2.isError = true;
       m2.setUpServer("127.0.0.2").getHandler();
       ManagedDevice p1 = ProjectorFactory.build("127.0.0.1");
       ManagedDevice p2 = ProjectorFactory.build("127.0.0.2");
       Thread.sleep(100);
       UnitDeviceModel modPC = new UnitDeviceModel();
       modPC.setUpServer("127.0.0.3",11211);
       ((UnitDeviceModel.Handler)modPC.setUpServer("127.0.0.3",11212).getHandler()).isError = false;
       Thread.sleep(500);
       modPC.setUpDaemon("127.0.0.3");
       ManagedDevice pc = VLCPlayer2ScreenFactory.build("127.0.0.3","11:11:11:11:11:11");
       //p1.switchOnCmd().exec();
       //p2.switchOnCmd().exec();
       //pc.switchOnCmd().exec();
       Module f = Module.newBuilder()
               .addSwitchable(0,"p1",p1)
               .addSwitchable(0,"p2",p2)
               .addSwitchable(1,"pc",pc)
               .build();
       f.switchOnCmd().exec();
       Thread.sleep(10000);
       f.switchOffCmd().exec();
       Thread.sleep(30000);

   }
    PJLinkDeviceModel m;

   NioSocketAcceptor acc;
    NioSocketAcceptor acc1;
    NioSocketAcceptor acc2;
    UnitDeviceModel modPC;
    UnitDeviceModel modPC1;
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
        Module f = Module.newBuilder()
                .addSwitchable(0,"p1",p1)
                .addSwitchable(0,"p2",p2)
                .addSwitchable(1,"pc",pc)
                .build();
        pc.stateReq().exec();
        f.switchOnCmd().exec();
        Thread.sleep(10000);
        f.switchOffCmd().exec();
        Thread.sleep(30000);

    }
    @Test
    public void testParallel() throws Exception{
        m = new PJLinkDeviceModel();
        m.isError = true;
        acc=m.setUpServer("127.0.0.1");
        ManagedDevice p1 = ProjectorFactory.build("127.0.0.1");
        acc1=m.setUpServer("127.0.0.2");
        ManagedDevice p2 = ProjectorFactory.build("127.0.0.2");
        Thread.sleep(100);
        modPC = new UnitDeviceModel();
        //acc1 = modPC.setUpServer("127.0.0.3",11211);
        acc2 = modPC.setUpDaemon("127.0.0.3");
        Thread.sleep(100);
        ManagedDevice pc = VLCPlayerFactory.buildDevice("127.0.0.3","11:11:11:11:11:11");
        modPC1 = new UnitDeviceModel();
        acc1 = modPC1.setUpServer("127.0.0.4",11211);
        acc2 = modPC1.setUpDaemon("127.0.0.4");
        Thread.sleep(100);
        ManagedDevice pc1 = VLCPlayerFactory.buildDevice("127.0.0.4","11:11:11:11:11:11");

        SwitchGroup f = SwitchGroup.newBuilder()
                .addSwitchable("p1",p1)
                .addSwitchable("p2",p2)
                .addSwitchable("pc1",pc)
                .addSwitchable("pc2",pc1)
                .build();
        f.switchOnCmd().exec().thenAccept((o)-> {
            HashMap<String,Object>obj =  (HashMap<String,Object>)o;
             logger.debug(obj.entrySet()
                        .stream()
                        .filter((e)->e.getValue()instanceof Throwable)
                        .collect(Collectors.toSet()));
        });
        Thread.sleep(10000);
        Thread.sleep(30000);


    }

    @Test
    public void testTwoJSON() throws Exception{
        m = new PJLinkDeviceModel();
        acc=m.setUpServer("127.0.0.1");
        //ManagedDevice p1 = ProjectorFactory.buildDevice("127.0.0.1");
        acc1=m.setUpServer("127.0.0.2");
       // ManagedDevice p2 = ProjectorFactory.buildDevice("127.0.0.2");
        Thread.sleep(100);
        modPC = new UnitDeviceModel();
        acc1 = modPC.setUpServer("127.0.0.3",11211);
        acc2 = modPC.setUpDaemon("127.0.0.3");
        Thread.sleep(100);

        String json = "" +
                "{\"devices\":"+
                "[" +
                    "{" +
                        "\"name\": \"PR-11.1\","+
                        "\"factory\": \"ProjectorFactory\"," +
                        "\"ip\": \"127.0.0.1\"," +
                        "\"description\": \"pr1\"" +
                    "}," +
                    "{" +
                        "\"name\": \"PR-11.2\","+
                        "\"factory\": \"ProjectorFactory\"," +
                        "\"ip\": \"127.0.0.2\"," +
                        "\"description\": \"pr2\"" +
                    "}," +
                    "{" +
                        "\"name\": \"BX-R1.1,PR-11.1_PR-11.2\","+
                        "\"factory\": \"VLCPlayerFactory\"," +
                        "\"mac\": \"11:11:11:11:11:11\"," +
                        "\"ip\": \"127.0.0.3\"," +
                        "\"description\": \"bx\"" +
                    "}" +
                "],"+

                "\"modules\": " +
                 "["+
                    "{" +
                        "\"name\": \"MOD:BX-R1.1,PR-11.1_PR-12.2\"," +
                        "\"sequence\":" +
                        "[" +
                            "[\"PR-11.1\"," + "\"PR-11.2\"],"+
                            "[\"BX-R1.1,PR-11.1_PR-11.2\"]" +
                        "]" +
                    "}" +
                "]" +
                "}";
        StructureReader reader = new StructureReader();
        reader.factories.add(new VLCPlayerFactory());
        reader.factories.add(new ProjectorFactory());
        HashSet<ManagedDevice> devices =  reader.loadDevicesFromJSON(json);
        HashSet<Module> modules = StructureReader.loadModulesFromJSON( devices,json);
        modules.stream().findFirst().get().switchOnCmd().exec();
        Thread.sleep(10000);
        modules.stream().findFirst().get().switchOnCmd().exec();
        Thread.sleep(1000);


    }
    @Test
    public void testSwitcherJSON() throws Exception{
        m = new PJLinkDeviceModel();
        acc=m.setUpServer("127.0.0.1");
        //ManagedDevice p1 = ProjectorFactory.buildDevice("127.0.0.1");
        acc1=m.setUpServer("127.0.0.2");
        // ManagedDevice p2 = ProjectorFactory.buildDevice("127.0.0.2");
        Thread.sleep(100);
        modPC = new UnitDeviceModel();
        acc1 = modPC.setUpServer("127.0.0.3",11211);
        acc2 = modPC.setUpDaemon("127.0.0.3");
        Thread.sleep(100);

        String json = "" +
                "{\"devices\":"+
                    "[" +
                        "{" +
                            "\"name\": \"PR-11.1\","+
                            "\"factory\": \"ProjectorFactory\"," +
                            "\"ip\": \"127.0.0.1\"," +
                            "\"description\": \"pr1\"" +
                        "}," +
                        "{" +
                            "\"name\": \"PR-11.2\","+
                            "\"factory\": \"ProjectorFactory\"," +
                            "\"ip\": \"127.0.0.2\"," +
                            "\"description\": \"pr2\"" +
                        "}," +
                        "{" +
                            "\"name\": \"BX-R1.1:PR-11.1\","+
                            "\"factory\": \"VLCPlayerFactory\"," +
                            "\"mac\": \"11:11:11:11:11:11\"," +
                            "\"ip\": \"127.0.0.3\"," +
                            "\"description\": \"bx\"" +
                        "}" +
                    "],"+
                "\"modules\": " +
                    "["+
                        "{" +
                            "\"name\": \"MOD:BX-R1.1:PR-11.1\"," +
                            "\"sequence\":" +
                            "[" +
                                "[\"PR-11.1\"],"+
                                "[\"BX-R1.1:PR-11.1\"]" +
                            "]" +
                        "}" +
                    "]," +
                "\"switchGroups\": " +
                    "["+
                        "{" +
                            "\"name\": \"SW:BX-R1.1,PR-12.2\"," +
                            "\"list\":" +
                            "[" +
                                "\"MOD:BX-R1.1:PR-11.1\"," + "\"PR-11.2\""+
                            "]" +
                        "}" +
                    "]" +
                "}";
        StructureReader reader = new StructureReader();
        reader.factories.add(new VLCPlayerFactory());
        reader.factories.add(new ProjectorFactory());
        HashSet<ManagedDevice> devices =  reader.loadDevicesFromJSON(json);
        HashSet<Module> modules = reader.loadModulesFromJSON( devices,json);
        HashSet<Switchable> switchables = new HashSet<>();
        switchables.addAll(modules);switchables.add(devices.stream().filter((dev)->dev.getName().equals("PR-11.2")).findFirst().get());
        HashSet<SwitchGroup> switchGroups = StructureReader.loadSwitchersFromJSON( switchables,json);
        switchGroups.stream().findFirst().get().switchOnCmd().exec();
        Thread.sleep(100000);
        switchGroups.stream().findFirst().get().switchOffCmd().exec().get();


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