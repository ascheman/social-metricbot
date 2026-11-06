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
package io.github.bmarwell.social.metricbot.db;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vidocq port: the DataSource arrives by CDI injection instead of a JNDI name.
 *
 * <p>On Open Liberty this was {@code @Resource(name = "jdbc/metricbotjpadatasource")}, resolved
 * against the {@code <dataSource>} declared in {@code server.xml}. Vidocq has no
 * {@code jakarta.annotation.Resource} support at all; instead the {@code mansart-pool} extension
 * builds the pool from the {@code vidocq.pool.*} properties and publishes it as the
 * {@code @Default DataSource} bean, so plain {@code @Inject} is the replacement.
 *
 * <p>Flyway is still driven programmatically from the injected DataSource. Vidocq does ship a
 * {@code vidocq-runtime-flyway-migration-extension}, which would be the more idiomatic route, but
 * keeping the existing code path means one less moving part in this migration.
 */
@Dependent
public class SocialMetricBotDataSource {

    private static final Logger LOG = LoggerFactory.getLogger(SocialMetricBotDataSource.class);

    private final AtomicBoolean initialized = new AtomicBoolean();

    /**
     * Indirect via {@code Instance<>} on purpose: the DataSource bean is contributed by the
     * mansart-pool extension at runtime, so it is not in this module's compile-time bean index and
     * Vauban's APT would reject a direct {@code @Inject DataSource} with "Unsatisfied dependency".
     * This mirrors the idiom used by Vidocq's own petstore example.
     */
    @Inject
    Instance<DataSource> dataSourceInstance;

    public void initialize() {
        if (this.initialized.get()) {
            return;
        }

        try {
            final Flyway flyway = Flyway.configure()
                    .dataSource(this.dataSourceInstance.get())
                    .locations("classpath:/io/github/bmarwell/social/metricbot/db/databasemigrations")
                    // .callbacks("io.github.bmarwell.social.metricbot.db.callbacks")
                    .load();

            flyway.migrate();
            this.initialized.compareAndSet(false, true);
        } catch (final FlywayException flywayException) {
            LOG.error("Unable to run flyway migration.", flywayException);
        }
    }
}
