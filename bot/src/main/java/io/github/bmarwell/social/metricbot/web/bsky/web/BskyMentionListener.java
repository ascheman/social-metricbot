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
package io.github.bmarwell.social.metricbot.web.bsky.web;

import io.github.bmarwell.social.metricbot.bsky.BlueSkyClient;
import io.github.bmarwell.social.metricbot.bsky.BskyStatus;
import io.github.bmarwell.social.metricbot.web.bsky.api.BskyProducer;
import io.github.bmarwell.social.metricbot.web.bsky.event.BskyMentionEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContextEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Vidocq port: @ApplicationScoped is REQUIRED here. On Open Liberty this class became a
// bean through beans.xml bean-discovery-mode="all" (CDI Full). Vauban is CDI Lite, which
// only recognises bean-defining annotations, so without this the class is not a bean and
// Foy never registers the listener — the application silently does nothing.
@ApplicationScoped
@WebListener
public class BskyMentionListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(BskyMentionListener.class);

    @Inject
    ScheduledExecutorService scheduler;

    @Inject
    ExecutorService executor;

    @Inject
    private BskyProducer bskyProducer;

    @Inject
    private Event<BskyMentionEvent> mentionEvent;

    private BlueSkyClient bsky;

    public BskyMentionListener() {
        // for injection
    }

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        ServletContextListener.super.contextInitialized(sce);
        LOG.info("init: [{}].", this);

        this.bsky = this.bskyProducer.produceBlueSky();

        // set up scheduler
        final ScheduledFuture<?> scheduledFuture =
                this.scheduler.scheduleWithFixedDelay(this::retrieveMentions, 10, 10, TimeUnit.SECONDS);

        if (scheduledFuture.isCancelled()) {
            throw new IllegalStateException("MentionListener did not start!" + scheduledFuture);
        }
    }

    protected void retrieveMentions() {
        // Vidocq port: copy() has no JDK equivalent; the executor now runs the dependent stage.
        final var unused = this.bsky.getRecentMentions()
                // no method returning void?
                .handle((final List<BskyStatus> result, final Throwable error) -> {
                    if (error != null) {
                        LOG.error("could not retrieve mentions: [" + error.getMessage() + "].", error);
                        return null;
                    }

                    for (final BskyStatus recentMention : result) {
                        if (LOG.isTraceEnabled()) {
                            LOG.trace("[BSKY] Found mention: " + recentMention);
                        }
                        mentionEvent.fire(new BskyMentionEvent(recentMention));
                    }

                    return null;
                });
    }
}
