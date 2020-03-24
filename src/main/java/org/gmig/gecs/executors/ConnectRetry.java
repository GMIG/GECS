package org.gmig.gecs.executors;

import org.apache.log4j.Logger;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Created by brix on 8/1/2018.
 */
class ConnectRetry implements IoFutureListener<ConnectFuture> {
    private int maxReconnectTries = 5;
    private int reconnectTimeMillis = 2000;

    private final Logger logger = Logger.getLogger(ConnectRetry.class);
    private final Supplier<ConnectFuture> operation;
    private final Supplier<Void> operationIfFailed;

    private final AtomicInteger i = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final String target;

    ConnectRetry(Supplier<ConnectFuture> operation, Supplier<Void> operationIfFailed, String target, int maxReconnectTries, int reconnectTimeMillis) {
        this.operation = operation;
        operation.get().addListener(this);
        this.target = target;
        this.operationIfFailed = operationIfFailed;
        this.maxReconnectTries = maxReconnectTries;
        this.reconnectTimeMillis = reconnectTimeMillis;
    }

    @Override
    public void operationComplete(ConnectFuture future) {
        int tr = i.getAndIncrement();
        if (!future.isConnected() && tr < maxReconnectTries) {
            scheduler.schedule(
                    () -> {
                        logger.debug(target + ":TCP reconnect retry " + i.get());
                        operation.get().addListener(this);
                    },
                    reconnectTimeMillis + (new Random().nextInt(50) + 20), TimeUnit.MILLISECONDS);
            return;
        }
        if (tr >= maxReconnectTries) {
            operationIfFailed.get();
            return;
        }
        IoSession session = future.getSession();
        logger.debug(target + "(IP:" + session.getRemoteAddress() + ")" + ":TCP connect success");

    }
}
