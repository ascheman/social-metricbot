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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.bmarwell.social.metricbot.common.MastodonConfig;
import java.util.concurrent.ScheduledExecutorService;
import jakarta.enterprise.event.Event;
import jakarta.servlet.ServletContextEvent;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class MastodonResponseProducerTest {

    @Mock
    MastodonConfig mastodonConfig;

    @Mock
    ScheduledExecutorService scheduler;

    @Mock
    UnprocessedMastodonStatusQueueHolder unprocessedMastodonStatusQueueHolder;

    @Mock
    @SuppressWarnings("rawtypes")
    Event processEvent;

    // Vidocq port: pre-initialised on purpose. Vauban weaves a
    // `protected MastodonResponseProducer(io.vidocq.vauban.api.ProxyLink)` constructor into every bean at
    // compile time. Mockito's @InjectMocks tries constructor injection first and picks the
    // BIGGEST constructor, so it built the object with a null ProxyLink and then skipped
    // field injection entirely — every @Mock stayed null. Assigning the field here forces
    // Mockito onto property/field injection, which is what this test wants.
    @InjectMocks
    MastodonResponseProducer producer = new MastodonResponseProducer();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void contextInitialized_doesNotSchedule_whenNotConfigured() {
        when(mastodonConfig.isConfigured()).thenReturn(false);

        producer.contextInitialized(mock(ServletContextEvent.class));

        verify(scheduler, never()).scheduleAtFixedRate(any(), anyLong(), anyLong(), any(TimeUnit.class));
    }
}
