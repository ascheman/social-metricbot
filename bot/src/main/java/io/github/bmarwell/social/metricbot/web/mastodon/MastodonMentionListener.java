/*
 * Copyright 2023-2026 The social-metricbot contributors
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
package io.github.bmarwell.social.metricbot.web.mastodon;

import io.github.bmarwell.social.metricbot.common.MastodonConfig;
import io.github.bmarwell.social.metricbot.mastodon.MastodonClient;
import io.github.bmarwell.social.metricbot.mastodon.MastodonStatus;
import io.github.bmarwell.social.metricbot.web.factory.MastodonProducer;
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
public class MastodonMentionListener implements ServletContextListener {

    private static final Logger LOG = LoggerFactory.getLogger(MastodonMentionListener.class);

    @Inject
    ScheduledExecutorService scheduler;

    @Inject
    ExecutorService executor;

    @Inject
    private MastodonProducer mastodonProducer;

    @Inject
    private MastodonConfig mastodonConfig;

    @Inject
    private Event<MastodonMentionEvent> mentionEvent;

    private MastodonClient mastodon;

    public MastodonMentionListener() {
        // injection constructor
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContextListener.super.contextInitialized(sce);
        LOG.info("init: [{}].", this);

        if (!this.mastodonConfig.isConfigured()) {
            LOG.warn("Mastodon is not configured — skipping mention listener setup.");
            return;
        }

        this.mastodon = this.mastodonProducer.produceMastodon();

        // set up scheduler
        ScheduledFuture<?> scheduledFuture =
                this.scheduler.scheduleWithFixedDelay(this::retrieveMentions, 10, 10, TimeUnit.SECONDS);

        if (scheduledFuture.isCancelled()) {
            throw new IllegalStateException("MentionListener did not start!" + scheduledFuture);
        }
    }

    protected void retrieveMentions() {
        // Vidocq port: copy() has no JDK equivalent; the executor now runs the dependent stage.
        final var unused = this.mastodon.getRecentMentions()
                // no method returning void?
                .handleAsync((List<MastodonStatus> result, Throwable error) -> {
                    if (error != null) {
                        LOG.error("could not retrieve mentions: [" + error.getMessage() + "].", error);
                        return null;
                    }

                    for (MastodonStatus recentMention : result) {
                        mentionEvent.fire(new MastodonMentionEvent(recentMention));
                    }

                    return null;
                });
    }
}
