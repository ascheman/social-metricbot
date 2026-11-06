/*
 * Copyright 2023 The social-metricbot contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.bmarwell.social.metricbot.web.concurrent;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Vidocq port: replaces Jakarta Concurrency, which Vidocq does not implement.
 *
 * <p>On Open Liberty the bot injected {@code ManagedExecutorService} and
 * {@code ManagedScheduledExecutorService} with {@code @Resource}; the container supplied them from
 * its own thread pools. Vidocq 0.4.0-RC2 ships no Concurrency extension — there is no such
 * extension in {@code vidocq-runtime-bom} and the package
 * {@code jakarta.enterprise.concurrent} does not appear anywhere in the Vidocq source tree — so
 * the executors are produced here instead.
 *
 * <h2>Behavioural difference, deliberately accepted</h2>
 *
 * <p>A container-managed executor <em>propagates context</em> (CDI scopes, transaction, security)
 * into the submitted task. These plain JDK executors do <strong>not</strong>. The scheduled tasks
 * of this bot fire CDI events whose observers are {@code @Transactional}, so a transaction is now
 * started by the observer itself rather than inherited from the caller. That is correct for this
 * application, but it is a real semantic change and not a mechanical substitution.
 *
 * <p>Jakarta Concurrency also offers {@code ManagedExecutorService.copy(CompletableFuture)}, which
 * returns a context-aware future. There is no {@code java.util.concurrent} equivalent at all; the
 * call sites were rewritten to use the original future and to pass this executor explicitly to the
 * dependent stage.
 *
 * <h2>Why the scheduler uses platform threads</h2>
 *
 * <p>Virtual threads should not be pooled, and a {@code ScheduledThreadPoolExecutor} is a pool of
 * long-lived threads. So the scheduler keeps a tiny platform-thread pool whose only job is timing,
 * while the actual work runs on {@link #virtualThreadExecutor()} — one virtual thread per task,
 * which is what the Vidocq ecosystem prescribes for I/O.
 */
@ApplicationScoped
public class BotExecutors {

    private static final int SCHEDULER_THREADS = 2;

    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            SCHEDULER_THREADS,
            Thread.ofPlatform().name("metricbot-scheduler-", 0).daemon(true).factory());

    @Produces
    @ApplicationScoped
    public ExecutorService virtualThreadExecutor() {
        return this.virtualThreadExecutor;
    }

    /**
     * {@code @Typed} is required: {@link ScheduledExecutorService} extends {@link ExecutorService},
     * so without it this producer also matches plain {@code ExecutorService} injection points and
     * Vauban rejects the build with "Ambiguous dependency".
     */
    @Produces
    @ApplicationScoped
    @Typed(ScheduledExecutorService.class)
    public ScheduledExecutorService scheduler() {
        return this.scheduler;
    }

    /**
     * The executors are shut down here rather than through {@code @Disposes} methods. Because
     * {@code ScheduledExecutorService} is an {@code ExecutorService}, a disposer declared for
     * either type matches both producers and Vauban fails at startup with "Multiple disposer
     * methods for producer ...#scheduler". Owning both instances as fields of this
     * {@code @ApplicationScoped} bean keeps the lifecycle unambiguous.
     */
    @PreDestroy
    void shutdownExecutors() {
        shutdownQuietly(this.scheduler);
        shutdownQuietly(this.virtualThreadExecutor);
    }

    private static void shutdownQuietly(final ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (final InterruptedException interruptedException) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
