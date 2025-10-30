package com.tidal.sdk.tidalapi.oauth2

import kotlin.concurrent.Volatile

/**
 * Simple in-memory implementation of TokenStorage.
 * This implementation is thread-safe but tokens are not persisted across application restarts.
 * For production use, consider implementing a persistent storage mechanism.
 */
class InMemoryTokenStorage : TokenStorage {
    @Volatile
    override var token: OAuth2Token? = null

    override fun saveToken(token: OAuth2Token?) {
        this.token = token
    }

    override fun clearToken() {
        this.token = null
    }
}
