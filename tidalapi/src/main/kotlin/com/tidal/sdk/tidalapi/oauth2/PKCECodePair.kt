package com.tidal.sdk.tidalapi.oauth2

/**
 * Holds PKCE (Proof Key for Code Exchange) parameters for Authorization Code flow.
 * This includes the code verifier, code challenge, and state parameter.
 */
class PKCECodePair
/**
 * Creates a new PKCE code pair.
 *
 * @param codeVerifier High-entropy cryptographic random string
 * @param codeChallenge SHA-256 hash of the code verifier (Base64-URL encoded)
 * @param state Random state parameter for CSRF protection
 */(
    /**
     * Gets the code verifier.
     * This is sent during the token exchange (Step 2).
     *
     * @return The code verifier
     */
    val codeVerifier: String,
    /**
     * Gets the code challenge.
     * This is sent during authorization URL generation (Step 1).
     *
     * @return The code challenge (SHA-256 hash of verifier)
     */
    val codeChallenge: String,
    /**
     * Gets the state parameter.
     * This is used for CSRF protection and must be validated on callback.
     *
     * @return The random state string
     */
    val state: String
) {
    override fun toString(): String {
        return "PKCECodePair{" +
                "state='" + state + '\'' +
                '}'
    }
}
