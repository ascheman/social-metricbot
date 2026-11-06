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

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;

/**
 * Vidocq port: the at-URIs are stored as {@code String} rather than {@link URI}.
 *
 * <p>On Open Liberty a {@code @Convert(converter = UriConverter.class)} mapped {@code URI} onto a
 * column. Mansart (Jakarta Data) has no {@code AttributeConverter} support and its dialects bind
 * only a fixed set of Java types, so the persistent fields are plain {@code String}. The
 * {@code URI}-typed accessors are kept, so callers are unaffected; conversion happens here.
 *
 * <p>The {@code @PrePersist} callback that used to null-sanitise the response URI is gone for the
 * same reason — Mansart does not run JPA lifecycle callbacks. {@link #setBotResponseAtUri(URI)}
 * already does that sanitising on every write, so behaviour is unchanged.
 */
@Entity
@Table(name = "BSKY_STATUS")
public class BskyStatusPdo {

    public static final Instant TIME_NOT_SET = Instant.EPOCH;

    public static final URI EMPTY_URI = URI.create("");

    private static final String EMPTY_URI_VALUE = "";

    @Id
    @Column(name = "STATUS_ATURI")
    private String atUri;

    @Column(name = "POST_TIME", nullable = false)
    private Instant postTime;

    @Column(name = "BOT_RESPONSE_ATURI", nullable = false)
    private String botResponseAtUri;

    @Column(name = "RESPONSE_TIME", nullable = false)
    private Instant responseTime;

    public BskyStatusPdo() {
        this.botResponseAtUri = EMPTY_URI_VALUE;
        this.responseTime = TIME_NOT_SET;
    }

    public BskyStatusPdo(final URI atUri, final Instant postTime) {
        this.atUri = atUri == null ? null : atUri.toString();
        this.postTime = postTime;
        this.botResponseAtUri = EMPTY_URI_VALUE;
        this.responseTime = TIME_NOT_SET;
    }

    public URI getAtUri() {
        return URI.create(this.atUri);
    }

    public void setAtUri(final URI atUri) {
        this.atUri = atUri == null ? null : atUri.toString();
    }

    public Instant getPostTime() {
        return postTime;
    }

    public void setPostTime(final Instant postTime) {
        this.postTime = postTime;
    }

    /**
     * NOTE: the {@code filter} below keeps the value only when it EQUALS the empty URI, which looks
     * inverted. It is preserved verbatim from the Open Liberty version so that the migration does
     * not change behaviour — it is reported separately rather than fixed here.
     */
    public Optional<URI> getBotResponseAtUri() {
        return Optional.ofNullable(this.botResponseAtUri)
                .map(URI::create)
                .filter(uri -> uri.equals(EMPTY_URI));
    }

    public void setBotResponseAtUri(final @Nullable URI botResponseAtUri) {
        if (botResponseAtUri == null) {
            this.botResponseAtUri = EMPTY_URI_VALUE;
            return;
        }
        this.botResponseAtUri = botResponseAtUri.toString();
    }

    public Instant getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(final Instant responseTime) {
        this.responseTime = responseTime;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof final BskyStatusPdo that)) {
            return false;
        }

        return getAtUri().equals(that.getAtUri());
    }

    @Override
    public int hashCode() {
        return getAtUri().hashCode();
    }
}
