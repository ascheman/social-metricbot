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
package io.github.bmarwell.social.metricbot.db.pdo;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BskyStatusPdoTest {

    private static final URI AT_URI = URI.create("at://did:plc:example/app.bsky.feed.post/3kabc");

    @Test
    void aStoredResponseUriIsReturned() {
        final var pdo = new BskyStatusPdo(AT_URI, Instant.EPOCH);
        pdo.setBotResponseAtUri(AT_URI);

        assertThat(pdo.getBotResponseAtUri()).contains(AT_URI);
    }

    @Test
    void anUnansweredStatusHasNoResponseUri() {
        final var pdo = new BskyStatusPdo(AT_URI, Instant.EPOCH);

        assertThat(pdo.getBotResponseAtUri()).isEmpty();
    }

    @Test
    void clearingTheResponseUriYieldsAnEmptyOptional() {
        final var pdo = new BskyStatusPdo(AT_URI, Instant.EPOCH);
        pdo.setBotResponseAtUri(AT_URI);

        pdo.setBotResponseAtUri(null);

        assertThat(pdo.getBotResponseAtUri()).isEmpty();
    }
}
