package com.tidal.sdk.tidalapi.oauth2

/**
 * Represents the different OAuth 2.0 flow types supported by the API.
 */
enum class OAuthFlow {
    /**
     * Client Credentials flow - Used for application-level authentication
     * where the client acts on its own behalf.
     */
    APPLICATION,

    /**
     * Resource Owner Password Credentials flow - Used when the user
     * provides their username and password directly to the client.
     */
    PASSWORD,

    /**
     * Authorization Code flow - The most secure flow for user authentication,
     * where the user is redirected to an authorization server.
     */
    ACCESS_CODE
}
