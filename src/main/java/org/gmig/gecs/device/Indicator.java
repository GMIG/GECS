package org.gmig.gecs.device;

import java.util.function.Consumer;

/**
 * Created by brix isOn 3/16/2018.
 */
public interface Indicator{
    void onTurnedOn (Consumer action);
    void onTurnedOff(Consumer action);
    void onError(Consumer<Throwable> action);
}
