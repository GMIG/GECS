package org.gmig.gecs.device;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by brix isOn 3/15/2018.
 */
public class RegularCheckManager {
    private Runnable check;
    private ScheduledExecutorService statusCheckScheduleExecutor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture future  = null;
    private int delayMillis = 3*60*1000;
    private AtomicBoolean stop = new AtomicBoolean(false);
    public int getDelayMillis() {
        return delayMillis;
    }
    public void setDelayMillis(int delayMillis) {
        this.delayMillis = delayMillis;
    }

    public synchronized void beginChecks( ){
        if(!stop.get()) {
            if (future != null)
                future.cancel(true);
        }
        stop.set(false);
        delayedCheck();
    }

    private synchronized void delayedCheck(){
        future = statusCheckScheduleExecutor.schedule(()->{
            if(!stop.get())
                check.run();
        },
                delayMillis + Double.valueOf(Math.random()*10).longValue(),TimeUnit.MILLISECONDS);
    }

    public synchronized void stopChecks(){
        stop.set(true);
        if(future!=null)
            future.cancel(true);
    }

    RegularCheckManager(ManagedDevice device) {
        device.stateReq().exception.add((o)->stopChecks());
        device.checkCmd().exception.add((o)->stopChecks());
        device.stateReq().success.add((o)-> {
            if (o.isOn())
                beginChecks();
            else
                stopChecks();
        });
        device.checkCmd().success.add((o)->this.delayedCheck());
        this.check = device.checkCmd()::exec;
        device.switchOffCmd().exception.add((o)->stopChecks());
        device.switchOnCmd().exception.add((o)->stopChecks());
        device.switchOffCmd().started.add(this::stopChecks);
        device.switchOnCmd().success.add((o)->beginChecks());
        if(device.checkedRestartCmd()!=null) {
            device.checkedRestartCmd().started.add(this::stopChecks);
            device.checkedRestartCmd().success.add((o)->beginChecks());
        }
    }
}
