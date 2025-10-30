package com.tidal.sdk.tidalapi.oauth2

/**
 * Interface for storing and retrieving OAuth2 tokens.
 * Implementations can provide different storage mechanisms (memory, disk, secure storage, etc.).
 */
interface TokenStorage {
    /**
     * Saves an OAuth2 token to storage.
     *
     * @param token The token to save
     */
    fun saveToken(token: OAuth2Token?)

    /**
     * Retrieves the stored OAuth2 token.
     *
     * @return The stored token, or null if no token is stored
     */
    val token: OAuth2Token?

    /**
     * Clears the stored token from storage.
     */
    fun clearToken()
}
