package com.tidal.sdk.tidalapi.oauth2

import org.apache.oltu.oauth2.client.OAuthClient
import org.apache.oltu.oauth2.client.request.OAuthClientRequest
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse
import org.apache.oltu.oauth2.common.exception.OAuthProblemException
import org.apache.oltu.oauth2.common.exception.OAuthSystemException
import org.apache.oltu.oauth2.common.message.types.GrantType
import java.io.IOException

/**
 * Manages OAuth2 token lifecycle including fetching and refreshing tokens.
 * This class is thread-safe and handles automatic token refresh before expiration.
 */
class OAuth2TokenManager @JvmOverloads constructor(
    private val config: OAuth2Config,
    private val tokenStorage: TokenStorage = InMemoryTokenStorage()
) {
    private val oAuthClient: OAuthClient = OAuthClient(OAuthOkHttpClient())
    private val tokenLock = Any()

    /**
     * Creates a new OAuth2TokenManager with custom token storage.
     *
     * @param config OAuth2 configuration
     * @param tokenStorage Custom token storage implementation
     */

    @get:Throws(IOException::class)
    val accessToken: String?
        /**
         * Gets a valid access token, automatically refreshing if needed.
         * This method is thread-safe and can be called from multiple threads.
         *
         * @return A valid access token
         * @throws IOException If token retrieval fails
         */
        get() {
            synchronized(tokenLock) {
                var token = tokenStorage.token
                    ?: throw IOException("No token available. User must authenticate first using Authorization Code flow.")

                // If token expired/expiring soon and we have refresh token, use it
                if (token.isExpiringSoon(TOKEN_REFRESH_BUFFER_MS)) {
                    if (token.refreshToken != null) {
                        token = refreshAccessToken(token.refreshToken!!)
                        tokenStorage.saveToken(token)
                    } else {
                        throw IOException("Token expired and no refresh token available. User must re-authenticate.")
                    }
                }
                return token.accessToken
            }
        }

    /**
     * Requests a new token using the configured OAuth2 flow.
     *
     * @return A new OAuth2Token
     * @throws IOException If the token request fails
     */
    @Throws(IOException::class)
    private fun requestNewToken(): OAuth2Token {
        try {
            val requestBuilder = OAuthClientRequest
                .tokenLocation(config.tokenUrl)
                .setClientId(config.clientId)
                .setClientSecret(config.clientSecret)

            // Set grant type based on flow
            when (config.flow) {
                OAuthFlow.APPLICATION -> requestBuilder.setGrantType(GrantType.CLIENT_CREDENTIALS)
                OAuthFlow.PASSWORD -> requestBuilder.setGrantType(GrantType.PASSWORD)
                OAuthFlow.ACCESS_CODE -> requestBuilder.setGrantType(GrantType.AUTHORIZATION_CODE)
                else -> requestBuilder.setGrantType(GrantType.CLIENT_CREDENTIALS)
            }

            if (config.scope != null && !config.scope.isEmpty()) {
                requestBuilder.setScope(config.scope)
            }

            val request = requestBuilder.buildBodyMessage()
            val response = oAuthClient.accessToken(request)

            if (response == null || response.accessToken == null) {
                throw IOException("Failed to obtain OAuth2 token: empty response")
            }

            val expiresIn = response.getExpiresIn()
            val expiresAt = System.currentTimeMillis() + (expiresIn * 1000)

            return OAuth2Token(
                response.accessToken,
                response.tokenType ?: "Bearer",
                expiresAt,
                response.refreshToken,
                response.scope
            )
        } catch (e: OAuthSystemException) {
            throw IOException("OAuth2 system error: " + e.message, e)
        } catch (e: OAuthProblemException) {
            throw IOException("OAuth2 problem: " + e.error + " - " + e.description, e)
        }
    }

    /**
     * Clears the stored token, forcing a new token to be fetched on next access.
     * Useful for logout or when a token is known to be invalid.
     */
    fun clearToken() {
        synchronized(tokenLock) {
            tokenStorage.clearToken()
        }
    }

    val currentToken: OAuth2Token?
        /**
         * Gets the current stored token without refreshing.
         * May return null or an expired token.
         *
         * @return The currently stored token, or null
         */
        get() {
            synchronized(tokenLock) {
                return tokenStorage.token
            }
        }

    /**
     * Sets credentials manually with access token, refresh token, and expiration time.
     * This is useful for restoring a previously saved session.
     *
     * @param accessToken The access token
     * @param refreshToken The refresh token
     * @param expiresAt The expiration timestamp in milliseconds since epoch
     */
    fun setCredentials(accessToken: String?, refreshToken: String?, expiresAt: Long) {
        synchronized(tokenLock) {
            val token = OAuth2Token(
                accessToken,
                "Bearer",
                expiresAt,
                refreshToken,
                null
            )
            tokenStorage.saveToken(token)
        }
    }

    val refreshToken: String?
        /**
         * Gets the refresh token from the currently stored token.
         *
         * @return The refresh token, or null if no token is stored
         */
        get() {
            synchronized(tokenLock) {
                val token = tokenStorage.token
                return token?.refreshToken
            }
        }

    val expiresAt: Long
        /**
         * Gets the expiration timestamp from the currently stored token.
         *
         * @return The expiration timestamp in milliseconds since epoch, or 0 if no token is stored
         */
        get() {
            synchronized(tokenLock) {
                val token = tokenStorage.token
                return token?.expiresAt ?: 0
            }
        }

    val isTokenExpired: Boolean
        /**
         * Checks if the current token is expired or expiring soon.
         *
         * @return true if the token is expired or will expire within the refresh buffer time, false otherwise
         */
        get() {
            synchronized(tokenLock) {
                val token = tokenStorage.token ?: return true
                return token.isExpiringSoon(TOKEN_REFRESH_BUFFER_MS)
            }
        }

    /**
     * Manually refreshes the current access token using the stored refresh token.
     * This method updates the stored token with the new access token.
     *
     * @throws IOException If the token refresh fails or no refresh token is available
     */
    @Throws(IOException::class)
    fun refreshToken() {
        synchronized(tokenLock) {
            val token = tokenStorage.token ?: throw IOException("No token available to refresh")

            if (token.refreshToken == null) {
                throw IOException("No refresh token available")
            }

            val newToken = refreshAccessToken(token.refreshToken!!)
            tokenStorage.saveToken(newToken)
        }
    }

    /**
     * Exchanges an authorization code for an access token (Authorization Code flow).
     * This is called after the user has logged in and been redirected back with an authorization code.
     *
     * @param authorizationCode The authorization code from the redirect URL
     * @param codeVerifier The PKCE code verifier (must match the challenge sent in authorization URL)
     * @param redirectUri The redirect URI (must match the one used in authorization URL)
     * @return OAuth2Token with access and refresh tokens
     * @throws IOException If the token exchange fails
     */
    @Throws(IOException::class)
    fun exchangeAuthorizationCode(
        authorizationCode: String?,
        codeVerifier: String?,
        redirectUri: String?
    ): OAuth2Token {
        try {
            val request = OAuthClientRequest
                .tokenLocation(config.tokenUrl)
                .setGrantType(GrantType.AUTHORIZATION_CODE)
                .setClientId(config.clientId)
                .setClientSecret(config.clientSecret)
                .setCode(authorizationCode)
                .setRedirectURI(redirectUri)
                .setParameter("code_verifier", codeVerifier)
                .buildBodyMessage()

            val response = oAuthClient.accessToken(request)

            if (response == null || response.accessToken == null) {
                throw IOException("Failed to exchange authorization code: empty response")
            }

            val token = createTokenFromResponse(response)

            // Store the token
            tokenStorage.saveToken(token)

            return token
        } catch (e: OAuthSystemException) {
            throw IOException("OAuth2 system error during code exchange: " + e.message, e)
        } catch (e: OAuthProblemException) {
            throw IOException("OAuth2 problem during code exchange: " + e.error + " - " + e.description, e)
        }
    }

    /**
     * Refreshes an access token using a refresh token.
     *
     * @param refreshToken The refresh token
     * @return New OAuth2Token with refreshed access token
     * @throws IOException If the token refresh fails
     */
    @Throws(IOException::class)
    private fun refreshAccessToken(refreshToken: String): OAuth2Token {
        try {
            val request = OAuthClientRequest
                .tokenLocation(config.tokenUrl)
                .setGrantType(GrantType.REFRESH_TOKEN)
                .setClientId(config.clientId)
                .setClientSecret(config.clientSecret)
                .setRefreshToken(refreshToken)
                .buildBodyMessage()

            val response = oAuthClient.accessToken(request)

            if (response == null || response.accessToken == null) {
                throw IOException("Failed to refresh token: empty response")
            }

            val newToken = createTokenFromResponse(response)
            if (newToken.refreshToken == null) {
                newToken.refreshToken = refreshToken;
            }

            // Preserve the original refresh token if response doesn't include a new one
            return newToken
        } catch (e: OAuthSystemException) {
            throw IOException("OAuth2 system error during token refresh: " + e.message, e)
        } catch (e: OAuthProblemException) {
            throw IOException("OAuth2 problem during token refresh: " + e.error + " - " + e.description, e)
        }
    }

    /**
     * Creates an OAuth2Token from the OAuth response.
     *
     * @param response The OAuth access token response
     * @return OAuth2Token object
     */
    private fun createTokenFromResponse(response: OAuthJSONAccessTokenResponse): OAuth2Token {
        val expiresIn = response.getExpiresIn()
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000)

        return OAuth2Token(
            response.accessToken,
            response.tokenType ?: "Bearer",
            expiresAt,
            response.refreshToken,
            response.scope
        )
    }

    /**
     * Shuts down the OAuth client.
     */
    fun shutdown() {
        oAuthClient.shutdown()
    }

    companion object {
        // Refresh token 60 seconds before expiration
        private const val TOKEN_REFRESH_BUFFER_MS: Long = 60000
    }
}
