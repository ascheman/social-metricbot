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
package io.github.bmarwell.social.metricbot.db.dao;

import io.github.bmarwell.social.metricbot.db.pdo.MastodonStatusPdo;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.time.Instant;
import java.util.Optional;

/**
 * Vidocq port: the public signatures are unchanged, so no caller in the bot module was touched.
 * Only the internals moved from {@code EntityManager} to Mansart's Jakarta Data repository.
 *
 * <p>Mapping of the JPA calls that are gone:
 *
 * <ul>
 *   <li>{@code entityManager.find(T, id)} → {@code findById(id)}
 *   <li>{@code entityManager.merge(e)} → {@code save(e)}, which already has upsert semantics, so
 *       the explicit find-then-insert-or-update branch is no longer needed
 *   <li>{@code entityManager.detach(e)} → dropped. Mansart returns detached instances; there is no
 *       persistence context to evict from.
 * </ul>
 */
@Dependent
public class MastodonStatusRepository implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Indirect via {@code Instance<>}: Mansart registers the generated repository implementation
     * through its own runtime SPI ({@code _MansartComponents}), not through Vauban's
     * compile-time bean index. A module that consumes this class therefore cannot resolve
     * MastodonStatusData statically, and a direct {@code @Inject} fails the build with
     * "Unsatisfied dependency". Same reason as the DataSource in SocialMetricBotDataSource.
     */
    @Inject
    Instance<MastodonStatusData> statusDataInstance;

    private MastodonStatusData statusData() {
        return this.statusDataInstance.get();
    }

    public Optional<MastodonStatusPdo> findById(final String id) {
        return this.statusData().findById(id);
    }

    public MastodonStatusPdo upsert(
            final String id,
            final Instant tweetTime,
            final @Nullable String botResponseId,
            final @Nullable Instant responseTime) {
        final MastodonStatusPdo pdo =
                this.statusData().findById(id).orElseGet(() -> new MastodonStatusPdo(id, tweetTime));

        pdo.setBotResponseId(botResponseId);
        pdo.setResponseTime(responseTime);

        this.statusData().save(pdo);

        return pdo;
    }
}
