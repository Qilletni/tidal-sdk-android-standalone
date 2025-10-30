package com.tidal.sdk.tidalapi.oauth2

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.*

/**
 * Generates PKCE (Proof Key for Code Exchange) parameters for Authorization Code flow.
 * Implements RFC 7636 to prevent authorization code interception attacks.
 */
object PKCEGenerator {
    private val SECURE_RANDOM = SecureRandom()
    private const val CODE_VERIFIER_LENGTH = 64 // 43-128 characters allowed
    private const val ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"

    /**
     * Generates a cryptographically random code verifier.
     * The code verifier is a high-entropy cryptographic random string using the
     * unreserved characters [A-Z] / [a-z] / [0-9] / "-" / "." / "_" / "~".
     *
     * @return Base64-URL encoded code verifier (43-128 characters)
     */
    fun generateCodeVerifier(): String {
        val randomBytes = ByteArray(CODE_VERIFIER_LENGTH)
        SECURE_RANDOM.nextBytes(randomBytes)

        val verifier = StringBuilder(CODE_VERIFIER_LENGTH)
        for (b in randomBytes) {
            val index = (b.toInt() and 0xFF) % ALLOWED_CHARS.length
            verifier.append(ALLOWED_CHARS[index])
        }

        return verifier.toString()
    }

    /**
     * Generates a code challenge from the code verifier using SHA-256.
     * code_challenge = BASE64URL(SHA256(ASCII(code_verifier)))
     *
     * @param codeVerifier The code verifier to hash
     * @return Base64-URL encoded SHA-256 hash of the code verifier
     * @throws IllegalStateException if SHA-256 algorithm is not available
     */
    fun generateCodeChallenge(codeVerifier: String): String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII))
            return base64UrlEncode(hash)
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalStateException("SHA-256 algorithm not available", e)
        }
    }

    /**
     * Generates a random state parameter for CSRF protection.
     * The state parameter is used to prevent cross-site request forgery attacks.
     *
     * @return Random state string (32 characters)
     */
    fun generateState(): String {
        val randomBytes = ByteArray(24) // 24 bytes = 32 base64 characters
        SECURE_RANDOM.nextBytes(randomBytes)
        return base64UrlEncode(randomBytes)
    }

    /**
     * Encodes bytes to Base64-URL format (RFC 4648).
     * Removes padding and uses URL-safe characters.
     *
     * @param data The bytes to encode
     * @return Base64-URL encoded string without padding
     */
    private fun base64UrlEncode(data: ByteArray?): String {
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(data)
    }
}
