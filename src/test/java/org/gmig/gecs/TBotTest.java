package org.gmig.gecs;

import org.gmig.gecs.command.ListenableCommand;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

/**
 * Created by brix on 1/22/2019.
 */
public class TBotTest {

    TBot bot = null;



    @Before
    public void setUp() throws Exception {
        HashSet<Long> hs = new HashSet<>();
        hs.add(-346908453L);
        bot = new TBot("747756215:AAE5v95TCfNa_Pdd2eVLog_dJS3xGmRKOl4","testExpo_bot", hs);
    }

    @Test
    public void testChoice() throws Exception {
        Thread.sleep(2000);

        ArrayList<String> list = new ArrayList<>();
        list.add("choice1");
        list.add("choice2");
        list.add("choice3");
        bot.addChoiceResponce("test","test request with list",list);
        bot.addTextResponce("choice3","choice 3 is wrong");
        bot.addActionResponce("choice2",new ListenableCommand<Void>(()->{
            System.out.println("Action!");
            return CompletableFuture.completedFuture(null);},"name"));

        Thread.sleep(30000);
    }

    @Test
    public void testRequest() throws Exception {
        bot.addTextResponce("test","test request");
        Thread.sleep(10000);
    }

    @Test
    public void testMessage() throws Exception {
        Thread.sleep(1000);
        bot.sendMessageToAllChats("Sending test");
    }


    @After
    public void cleanUp() throws Exception{
        bot.stop();
    }
}