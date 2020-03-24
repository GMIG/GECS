package org.gmig.gecs.groups;

import org.apache.log4j.Logger;
import org.gmig.gecs.executors.TCPReactionHandler;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Created by brix on 1/15/2019.
 */
public class Watchdog {

    private static final Logger logger = Logger.getLogger(Watchdog.class);

    public final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicReference<LocalDateTime> time = new AtomicReference<>(LocalDateTime.MIN);

    public final ConcurrentLinkedQueue<Consumer<Throwable>> onRestartedBySwitchOn= new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<Consumer<Throwable>> onKilledBySwitchOn= new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<Consumer<Throwable>> onRestartedByCheck= new ConcurrentLinkedQueue<>();
    public final ConcurrentLinkedQueue<Consumer<Throwable>> onKilledByCheck= new ConcurrentLinkedQueue<>();

    public final VisModule module;

    private void notifyListeners(ConcurrentLinkedQueue<Consumer<Throwable>> listeners, Throwable e){
        for (Consumer<Throwable> listener : listeners) {
            try{
                listener.accept(e);
            }
            catch (Exception ex){
                logger.warn("Watchdog listener returned exception" );
            }

        }
    }


    public Watchdog(VisModule module) {

        this.module = module;
        if(module.getSource().checkedRestartCmd() != null) {

            module.getSource().switchOnCmd().exception.add((e) -> {
                if (enabled.get())
                    if (e.getMessage().contains(TCPReactionHandler.connectionNotCreatedID) || e.getMessage().contains(TCPReactionHandler.connectionTimeoutID)) {
                        notifyListeners(onRestartedBySwitchOn, e);
                        logger.warn("Watchdog restarting " + module.getSource().getName() + " on Switch on. " + "Error " + e.getMessage());
                        module.getSource().checkedRestartCmd().exec().handle((o, ex) -> {
                            if (ex != null) {
                                notifyListeners(onKilledBySwitchOn, ex);
                                logger.warn("Watchdog killed " + module.getName() + " on Switch on. " + "Error " + ex.getMessage());
                                module.getVisualisers().forEach((v) -> {
                                    v.switchOffCmd().exec();
                                    logger.warn(v.getName() + " killed");
                                });
                            } else
                                logger.warn("Watchdog restart on Switch On successful " + module.getSource().getName() + ". " + "Returned " + o);
                            return null;
                        });
                    }
            });

            module.getSource().checkCmd().exception.add((e) -> {
                if (enabled.get())
                    if (e.getMessage().contains(TCPReactionHandler.connectionNotCreatedID) || e.getMessage().contains(TCPReactionHandler.connectionTimeoutID)) {
                        if (time.get().until(LocalDateTime.now(), ChronoUnit.MINUTES) > 30) {
                            time.set(LocalDateTime.now());
                            notifyListeners(onRestartedByCheck, e);
                            logger.warn("Watchdog restarting " + module.getSource().getName() + " on Check. " + "Error " + e.getMessage());
                            module.getSource().checkedRestartCmd().exec();
                        } else {
                            notifyListeners(onKilledByCheck, e);
                            logger.warn("Watchdog kills " + module.getName() + " on Check. " + "Error " + e.getMessage());
                            module.getVisualisers().forEach((v) -> v.switchOffCmd().exec());
                        }
                    }
            });
        }
        else {
            module.getSource().switchOnCmd().exception.add((e) -> {
                notifyListeners(onKilledBySwitchOn, e);
                logger.warn("Watchdog kills " + module.getName() + " on switch on. " + "Error " + e.getMessage());
                module.getVisualisers().forEach((v) -> v.switchOffCmd().exec());
            });
        }

    }
}
