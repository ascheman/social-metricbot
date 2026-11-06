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
package io.github.bmarwell.social.metricbot.web;

import io.vidocq.runtime.core.Vidocq;

/**
 * Vidocq port: the entry point.
 *
 * <p>On Open Liberty this application was a war deployed into a running server. Vidocq inverts
 * that — the application is an executable Java module that boots the runtime, which then discovers
 * every extension on the module path and starts them in priority order (pool, data, REST, and the
 * Chappe HTTP listener last).
 */
public final class MetricBotApp {

    private MetricBotApp() {
        // not instantiable
    }

    public static void main(final String[] args) {
        Vidocq.main(args);
    }
}
