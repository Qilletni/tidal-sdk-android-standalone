package com.tidal.sdk.tidalapi.oauth2

/**
 * Configuration for OAuth2 authentication.
 * Use the Builder to construct instances.
 */
class OAuth2Config private constructor(builder: Builder) {
    val clientId: String?
    val clientSecret: String?
    val authorizationUrl: String?
    val tokenUrl: String?
    val scope: String?
    val flow: OAuthFlow?

    init {
        this.clientId = builder.clientId
        this.clientSecret = builder.clientSecret
        this.authorizationUrl = builder.authorizationUrl
        this.tokenUrl = builder.tokenUrl
        this.scope = builder.scope
        this.flow = builder.flow
    }

    /**
     * Builder for constructing OAuth2Config instances.
     */
    class Builder {
        var clientId: String? = null
        var clientSecret: String? = null
        var authorizationUrl: String? = "https://login.tidal.com/"
        var tokenUrl = "https://auth.tidal.com/v1/oauth2/token"
        var scope: String? = "user.read"
        var flow: OAuthFlow? = OAuthFlow.ACCESS_CODE // Authorization Code by default

        fun clientId(clientId: String): Builder {
            this.clientId = clientId
            return this
        }

        fun clientSecret(clientSecret: String): Builder {
            this.clientSecret = clientSecret
            return this
        }

        fun authorizationUrl(authorizationUrl: String?): Builder {
            this.authorizationUrl = authorizationUrl
            return this
        }

        fun tokenUrl(tokenUrl: String): Builder {
            this.tokenUrl = tokenUrl
            return this
        }

        fun scope(scope: String?): Builder {
            this.scope = scope
            return this
        }

        fun flow(flow: OAuthFlow?): Builder {
            this.flow = flow
            return this
        }

        fun build(): OAuth2Config {
            require(!(clientId == null || clientId!!.isEmpty())) { "clientId is required" }
            require(!((clientSecret == null) || clientSecret!!.isEmpty())) { "clientSecret is required" }
            require(!(tokenUrl.isEmpty())) { "tokenUrl is required" }
            return OAuth2Config(this)
        }
    }

    override fun toString(): String {
        return "OAuth2Config{" +
                "clientId='" + clientId + '\'' +
                ", authorizationUrl='" + authorizationUrl + '\'' +
                ", tokenUrl='" + tokenUrl + '\'' +
                ", scope='" + scope + '\'' +
                ", flow=" + flow +
                '}'
    }
}
