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
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;
import jakarta.transaction.Transactional;

/**
 * Vidocq port: Mansart generates the implementation of this interface at compile time and Vauban
 * publishes it as a bean. It replaces the hand-written {@code EntityManager} access that Open
 * Liberty provided — Vidocq implements Jakarta Data, not Jakarta Persistence.
 *
 * <p>The id type is {@code String}, matching the persistent field of {@link MastodonStatusPdo}; the
 * {@code URI}-based API stays on {@link MastodonStatusRepository}.
 */
@Transactional
@Repository
public interface MastodonStatusData extends BasicRepository<MastodonStatusPdo, String> {}
