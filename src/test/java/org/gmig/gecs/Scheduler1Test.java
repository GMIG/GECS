package org.gmig.gecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gmig.gecs.command.ListenableCommand;
import org.gmig.gecs.device.Switchable;
import org.gmig.gecs.groups.SwitchGroup;
import org.gmig.gecs.groups.SwitchGroupScheduler;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Created by brix on 4/24/2018.
 */
public class Scheduler1Test {
    private static final Logger logger = Logger.getLogger(Scheduler1Test.class);

    @Test
    public void schedule1() throws Exception {
        HashSet<SwitchGroup> switchGroups = new HashSet<>();
        switchGroups.add(SwitchGroup.newBuilder().addSwitchable("sw",new Switchable(){
            @Override
            public ListenableCommand<?> switchOnCmd() {
                return new ListenableCommand<Void>(()->{logger.debug("Firing SwitchOn");return CompletableFuture.completedFuture(null);},"switchOn");
            }
            @Override
            public ListenableCommand<?> switchOffCmd() {
                return new ListenableCommand<Void>(()->{logger.debug("Firing SwitchOff");return CompletableFuture.completedFuture(null);},"switchOff");
            }
            @Override
            public String getName() {
                return "mainExposition";
            }

            @Override
            public Set<Switchable> getChildren() {
                return null;
            }

        }).setName("mainExposition").build());
        SwitchGroupScheduler s = new SwitchGroupScheduler(switchGroups);

        String spec = "[\n" +
                "        {\n" +
                "          \"switchGroup\": \"mainExposition\",\n" +
                "          \"date\": \"26 4 2018\",\n" +
                "          \"switch on\": \"0 20 13\",\n" +
                "          \"switch off\": \"0 0 14\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"switchGroup\": \"mainExposition\",\n" +
                "          \"date\": \""+
                new SimpleDateFormat("d M y")
                        .format(Date.from(Instant.now())) +"\",\n" +
                "          \"switch on\": \""+
                new SimpleDateFormat("s m H")
                        .format(Date.from(Instant.now().plus(10,ChronoUnit.SECONDS)))+"\",\n" +
                "          \"switch off\": \"0 0 14\"\n" +
                "        },\n" +

                "        {\n" +
        "          \"switchGroup\": \"mainExposition\",\n" +
        "          \"date\": \"10 5 2018\",\n" +
        "          \"switch on\": \"0 0 12\",\n" +
        "          \"switch off\": \"0 0 13\"\n" +
        "        },\n" +
        "        {\n" +
        "          \"switchGroup\": \"mainExposition\",\n" +
        "          \"date\": \"20 5 2018\",\n" +
        "          \"switch on\": \"0 0 15\",\n" +
        "          \"switch off\": \"0 0 16\"\n" +
                "        },\n" +
        "        {\n" +
        "          \"switchGroup\": \"mainExposition\",\n" +
        "          \"date\": \"23 5 2018\"\n" +
        "        }\n" +
                "]";
        String sched = "[\n" +
                "    {\n" +
                "    \"switchGroup\":\n" +
                "        \"mainExposition\",\n" +
                "    \"switch on\": {\n" +
                "        \"include\": [\n" +
                "          \"0 0 9 ? * TUE,THU,FRI,SAT,SUN\",\n" +
                "          \"0 0 10 ? * WED\"\n" +
                "        ],\n" +
                "        \"exclude\": [\n" +
                "          \"* * * ? * FRIL\"\n" +
                "        ]\n" +
                "      },\n" +
                "    \"switch off\": {\n" +
                "        \"include\": [\n" +
                "          \"0 0 19 ? * TUE,THU,FRI,SAT,SUN\",\n" +
                "          \"0 0 20 ? * WED\"\n" +
                "        ],\n" +
                "        \"exclude\": [\n" +
                "          \"* * * ? * FRIL\"\n" +
                "        ]\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "    \"switchGroup\":\n" +
                "        \"cafe\",\n" +
                "    \"switch on\": {\n" +
                "        \"include\": [\n" +
                "          \"0 0 9 ? * TUE,THU,FRI,SAT,SUN\",\n" +
                "          \"0 0 10 ? * WED\"\n" +
                "        ],\n" +
                "        \"exclude\": [\n" +
                "          \"* * * ? * FRIL\"\n" +
                "        ]\n" +
                "      },\n" +
                "    \"switch off\": {\n" +
                "        \"include\": [\n" +
                "          \"0 0 19 ? * TUE,THU,FRI,SAT,SUN\",\n" +
                "          \"0 0 20 ? * WED\"\n" +
                "        ],\n" +
                "        \"exclude\": [\n" +
                "          \"* * * ? * FRIL\"\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +

                "]";

        ObjectMapper f = new ObjectMapper();
        s.loadSchedule(sched,spec);
        s.getDates(Date.from(Instant.now()),Date.from(Instant.now().plus(20, ChronoUnit.DAYS)));
        Thread.sleep(60000);



    }

}