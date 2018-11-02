/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.webserver.synchronous;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import io.helidon.webserver.ServerRequest;

/**
 * Synchronous method support for Web Server.
 */
public class Sync {
    private final ExecutorService executorService;

    Sync(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public static <T> CompletableFuture<T> submit(ServerRequest req, Supplier<T> supplier) {

        Sync sync = req.context().get(Sync.class)
                .orElseThrow(() -> new SyncExecutionException("SyncSupport is not properly configured. Please add "
                                                                      + "routingBuilder.register(SyncSupport.create()) to your "
                                                                      + "setup"));

        AtomicReference<Future<?>> futureRef = new AtomicReference<>();

        CompletableFuture<T> result = new CompletableFuture<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                Future<?> future = futureRef.get();
                if (null != future) {
                    super.cancel(mayInterruptIfRunning);
                    return future.cancel(mayInterruptIfRunning);
                } else {
                    return false;
                }
            }
        };

        Future<?> future = sync.executorService.submit(() -> {
            try {
                T theResult = supplier.get();
                result.complete(theResult);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });

        futureRef.set(future);

        return result;
    }

    public static CompletableFuture<Void> accept(ServerRequest req, Runnable runnable) {
        return submit(req, () -> {
            runnable.run();
            return null;
        });
    }
}
