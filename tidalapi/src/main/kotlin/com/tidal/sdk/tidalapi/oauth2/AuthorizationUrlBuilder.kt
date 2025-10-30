package com.tidal.sdk.tidalapi.oauth2

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.RuntimeException

/**
 * Builds authorization URLs for OAuth2 Authorization Code flow with PKCE.
 */
class AuthorizationUrlBuilder
/**
 * Creates a new authorization URL builder.
 *
 * @param config OAuth2 configuration
 */(private val config: OAuth2Config) {
    /**
     * Builds the complete authorization URL for user login.
     * This URL should be opened in a browser where the user will log in.
     *
     * @param pkceCodePair PKCE code challenge and state
     * @param redirectUri The redirect URI registered with TIDAL
     * @param scopes Requested OAuth2 scopes
     * @return Complete authorization URL
     */
    fun buildAuthorizationUrl(pkceCodePair: PKCECodePair, redirectUri: String, vararg scopes: String?): String {
        val url = StringBuilder(config.authorizationUrl)

        // Remove trailing slash if present
        if (url.get(url.length - 1) == '/') {
            url.setLength(url.length - 1)
        }

        url.append("/authorize?")
        url.append("response_type=code")
        url.append("&client_id=").append(urlEncode(config.clientId!!))
        url.append("&redirect_uri=").append(urlEncode(redirectUri))

        // Add scopes if provided
        if (scopes.isNotEmpty()) {
            url.append("&scope=").append(urlEncode(scopes.filterNotNull().joinToString(" ")))
        }

        // Add PKCE parameters
        url.append("&code_challenge=").append(urlEncode(pkceCodePair.codeChallenge))
        url.append("&code_challenge_method=S256")
        url.append("&state=").append(urlEncode(pkceCodePair.state))

        return url.toString()
    }

    /**
     * URL-encodes a string parameter.
     *
     * @param value The value to encode
     * @return URL-encoded string
     */
    private fun urlEncode(value: kotlin.String): kotlin.String? {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
        } catch (e: UnsupportedEncodingException) {
            // UTF-8 is always supported
            throw RuntimeException("UTF-8 encoding not supported", e)
        }
    }
}
