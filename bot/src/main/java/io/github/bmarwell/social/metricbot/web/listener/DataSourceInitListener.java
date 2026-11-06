/*
 * Copyright 2021-2023 The social-metricbot contributors
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
package io.github.bmarwell.social.metricbot.web.listener;

import io.github.bmarwell.social.metricbot.db.SocialMetricBotDataSource;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContextEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

// Vidocq port: @ApplicationScoped is REQUIRED here. On Open Liberty this class became a
// bean through beans.xml bean-discovery-mode="all" (CDI Full). Vauban is CDI Lite, which
// only recognises bean-defining annotations, so without this the class is not a bean and
// Foy never registers the listener — the application silently does nothing.
@ApplicationScoped
@WebListener
public class DataSourceInitListener implements ServletContextListener {

    @Inject
    private SocialMetricBotDataSource socialMetricBotDataSource;

    @Override
    public void contextInitialized(final ServletContextEvent sce) {
        this.socialMetricBotDataSource.initialize();
    }

    @Override
    public void contextDestroyed(final ServletContextEvent sce) {}
}
