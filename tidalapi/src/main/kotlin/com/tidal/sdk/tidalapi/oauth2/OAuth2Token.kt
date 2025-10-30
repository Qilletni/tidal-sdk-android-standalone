package com.tidal.sdk.tidalapi.oauth2

/**
 * Represents an OAuth2 access token with expiration information.
 * This class is immutable and thread-safe.
 */
class OAuth2Token(
    val accessToken: String?,
    val tokenType: String?,
    val expiresAt: Long, // timestamp in milliseconds since epoch
    var refreshToken: String?, // optional, may be null
    val scope: String?
) {

    val isExpired: Boolean

    /**
     * Checks if the token has expired.
     *
     * @return true if the current time is past the expiration time
     */
    get() = System.currentTimeMillis() >= expiresAt

    /**
     * Checks if the token is expiring soon.
     * This is useful for proactive token refresh before actual expiration.
     *
     * @param bufferMillis Time buffer in milliseconds before expiration
     * @return true if token will expire within the buffer period
     */
    fun isExpiringSoon(bufferMillis: Long): Boolean {
        return System.currentTimeMillis() >= (expiresAt - bufferMillis)
    }

    override fun toString(): String {
        return "OAuth2Token{" +
                "tokenType='" + tokenType + '\'' +
                ", expiresAt=" + expiresAt +
                ", scope='" + scope + '\'' +
                ", hasRefreshToken=" + (refreshToken != null) +
                '}'
    }
}
