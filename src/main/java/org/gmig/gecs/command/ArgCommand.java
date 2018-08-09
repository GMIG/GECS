package org.gmig.gecs.command;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Created by brix on 4/20/2018.
 */
//@FunctionalInterface
public interface ArgCommand <U,T> extends Function<U,CompletableFuture<T>> {}
