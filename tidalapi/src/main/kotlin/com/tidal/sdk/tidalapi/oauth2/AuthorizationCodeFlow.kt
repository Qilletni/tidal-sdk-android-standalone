package com.tidal.sdk.tidalapi.oauth2

import java.io.IOException

/**
 * Handles the complete OAuth2 Authorization Code flow with PKCE.
 *
 * This class provides a simple two-step authentication process:
 * 1. Call initializeLogin() to get the authorization URL
 * 2. Call finalizeLogin() with the authorization code from the callback
 *
 * Example usage:
 * <pre>
 * // Step 1: Get login URL
 * String loginUrl = authFlow.initializeLogin("myapp://callback", "user.read", "playlists.read");
 * // Open loginUrl in browser
 *
 * // Step 2: After redirect, complete login
 * OAuth2Token token = authFlow.finalizeLogin(authCode, state, "myapp://callback");
</pre> *
 */
class AuthorizationCodeFlow
/**
 * Creates a new AuthorizationCodeFlow handler.
 *
 * @param config OAuth2 configuration
 * @param tokenManager Token manager for storing and refreshing tokens
 */(
    private val config: OAuth2Config,
    /**
     * Gets the token manager associated with this flow.
     * Use this to create the TidalApiClient after authentication is complete.
     *
     * @return The OAuth2TokenManager
     */
    val tokenManager: OAuth2TokenManager
) {
    private var currentPKCE: PKCECodePair? = null

    /**
     * Step 1: Initialize the login flow.
     * Generates PKCE parameters and builds the authorization URL.
     * Open this URL in a browser to allow the user to log in.
     *
     * @param redirectUri Your application's redirect URI (must be registered with TIDAL)
     * @param scopes Requested OAuth2 scopes (e.g., "user.read", "playlists.read")
     * @return Authorization URL to open in browser
     */
    fun initializeLogin(redirectUri: String, vararg scopes: String?): String {
        // Generate PKCE code verifier and challenge
        val codeVerifier = PKCEGenerator.generateCodeVerifier()
        val codeChallenge = PKCEGenerator.generateCodeChallenge(codeVerifier)
        val state = PKCEGenerator.generateState()

        // Store PKCE parameters for later use
        currentPKCE = PKCECodePair(codeVerifier, codeChallenge, state)

        // Build authorization URL
        val builder = AuthorizationUrlBuilder(config)
        return builder.buildAuthorizationUrl(currentPKCE!!, redirectUri, *scopes)
    }

    /**
     * Step 2: Complete the login after user has been redirected back.
     * Exchanges the authorization code for access and refresh tokens.
     *
     * @param authorizationCode The authorization code from the callback URL
     * @param state The state parameter from the callback URL (for CSRF protection)
     * @param redirectUri The redirect URI (must match the one used in initializeLogin)
     * @return OAuth2Token with access and refresh tokens
     * @throws IOException If the token exchange fails
     * @throws IllegalStateException If state parameter doesn't match (possible CSRF attack)
     * @throws IllegalStateException If initializeLogin() was not called first
     */
    @Throws(IOException::class)
    fun finalizeLogin(authorizationCode: String?, state: String?, redirectUri: String?): OAuth2Token? {
        // Validate that initializeLogin was called
        checkNotNull(currentPKCE) { "initializeLogin() must be called before finalizeLogin()" }

        // Validate state parameter (CSRF protection)
        check(currentPKCE!!.state == state) {
            "State parameter mismatch. Expected: " + currentPKCE!!.state +
                    ", Got: " + state + ". Possible CSRF attack detected."
        }

        try {
            // Exchange authorization code for tokens
            val token = tokenManager.exchangeAuthorizationCode(
                authorizationCode,
                currentPKCE!!.codeVerifier,
                redirectUri
            )

            return token
        } finally {
            // Clear PKCE data for security
            currentPKCE = null
        }
    }

    val isLoginInProgress: Boolean
        /**
         * Checks if a login flow is currently in progress.
         *
         * @return true if initializeLogin() has been called but finalizeLogin() has not
         */
        get() = currentPKCE != null

    /**
     * Cancels the current login flow.
     * Call this if the user cancels the login or if you need to start over.
     */
    fun cancelLogin() {
        currentPKCE = null
    }
}
