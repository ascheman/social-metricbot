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

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.github.bmarwell.social.metricbot.db.pdo.BskyStatusPdo;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;

/**
 * Vidocq port: the public signatures are unchanged, so no caller in the bot module was touched.
 * Only the internals moved from {@code EntityManager} to Mansart's Jakarta Data repository.
 *
 * <p>This class is also where {@code URI} is translated to the {@code String} that
 * {@link BskyStatusPdo} now persists, replacing the {@code UriConverter} that Mansart cannot run.
 *
 * <p>{@code merge} became {@code save} (already upsert semantics) and {@code detach} was dropped —
 * Mansart hands back detached instances, so there is no persistence context to evict from.
 */
@Dependent
@Default
public class BskyStatusRepository implements Serializable {

    @Serial
    private static final long serialVersionUID = 6270206030149625623L;

    /**
     * Indirect via {@code Instance<>}: Mansart registers the generated repository implementation
     * through its own runtime SPI ({@code _MansartComponents}), not through Vauban's
     * compile-time bean index. A module that consumes this class therefore cannot resolve
     * BskyStatusData statically, and a direct {@code @Inject} fails the build with
     * "Unsatisfied dependency". Same reason as the DataSource in SocialMetricBotDataSource.
     */
    @Inject
    Instance<BskyStatusData> statusDataInstance;

    private BskyStatusData statusData() {
        return this.statusDataInstance.get();
    }

    public Optional<BskyStatusPdo> findByAtUri(final URI postAtUri) {
        return this.statusData().findById(postAtUri.toString());
    }

    @CanIgnoreReturnValue
    public BskyStatusPdo upsert(
            final URI bskyStatusAtUri,
            final Instant statusCreatedAt,
            final @Nullable URI botResponseAtUri,
            final @Nullable Instant repliedAt) {
        final BskyStatusPdo pdo = this.statusData()
                .findById(bskyStatusAtUri.toString())
                .orElseGet(() -> new BskyStatusPdo(bskyStatusAtUri, statusCreatedAt));

        pdo.setBotResponseAtUri(botResponseAtUri);
        pdo.setResponseTime(repliedAt);

        this.statusData().save(pdo);

        return pdo;
    }
}
