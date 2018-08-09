package org.gmig.gecs;

import org.gmig.gecs.executors.WOLCommandExecutor;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by brix isOn 2/22/2018.
 */
public class WOLCommandExecutorTest {

    private static final Logger logger = Logger.getLogger(WOLCommandExecutorTest.class);

    @Test
    public void testSendPacket() throws IOException, ExecutionException, InterruptedException {
        WOLCommandExecutor cmd = new WOLCommandExecutor();
        CompletableFuture<Void> f = cmd.sendWAL(("10.8.1.5"), 0, "fc:aa:14:f3:35:e0");
        f.get();
        assertFalse(f.isCompletedExceptionally());
    }
    @Test
    public void testSendPacketModel() throws IOException, ExecutionException, InterruptedException {
        WOLCommandExecutor cmd = new WOLCommandExecutor();
        CompletableFuture<Void> f = cmd.sendWAL(("127.0.0.3"), 0, "11:11:11:11:11:11");
        f.get();
        assertFalse(f.isCompletedExceptionally());
    }

    @Test (expected = ExecutionException.class)
    public void testUnreachableRetry() throws IOException, ExecutionException {
        WOLCommandExecutor cmd = new WOLCommandExecutor();
        cmd.setIpResendTimeMillis(500);
        cmd.setIpResendTries(3);
        CompletableFuture<Void> f = cmd.sendWAL(("100.100.100.100"), 0, "11:11:11:11:11:11");
        try {
            f.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            assertTrue(f.isCompletedExceptionally());
        }
    }

    public static <T>
    CompletableFuture<T> anyOf(List<? extends CompletionStage<? extends T>> l) {

        CompletableFuture<T> f=new CompletableFuture<>();
        Consumer<T> complete=f::complete;
        CompletableFuture.allOf(
                l.stream().map(s -> s.thenAccept(complete)).toArray(CompletableFuture<?>[]::new)
        ).exceptionally(ex -> { f.completeExceptionally(ex); return null; });
        return f;
    }

    @Test(expected = ExecutionException.class)
    public void testMultipleSends() throws ExecutionException, IOException, InterruptedException {
        WOLCommandExecutor cmd = new WOLCommandExecutor();
        ArrayList<CompletableFuture<Void>> streamGenerated =  new ArrayList<>();
        int n=10;
        cmd.setIpResendTries(3);
        cmd.setIpResendTimeMillis(500);
        for(int i=0;i<n;i++){
            streamGenerated.add(cmd.sendWAL("128.0.0."+i, 0, "11:11:11:11:11:11"));
        }
        CompletableFuture<Void> [] streamGeneratedArray = new CompletableFuture [n];
        streamGenerated.toArray(streamGeneratedArray);
        CompletableFuture<Object> all = anyOf(streamGenerated);
        all.get();
    }


}