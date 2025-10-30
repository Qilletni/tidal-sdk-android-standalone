package com.tidal.sdk.tidalapi.oauth2

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * OkHttp interceptor that adds OAuth2 Bearer token to requests
 * and handles 401/403 responses by refreshing the token and retrying.
 */
class OAuth2Interceptor
/**
 * Creates a new OAuth2Interceptor.
 *
 * @param tokenManager The token manager to use for obtaining tokens
 */(private val tokenManager: OAuth2TokenManager) : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Skip if request already has authorization header
        if (request.header("Authorization") != null) {
            return chain.proceed(request)
        }

        // Get token and add to request
        val token = tokenManager.accessToken
        val authenticatedRequest = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        // Execute request
        var response = chain.proceed(authenticatedRequest)

        // If 401/403, try refreshing token and retry once
        if (response.code == 401 || response.code == 403) {
            // Close the failed response
            response.close()

            // Clear cached token to force refresh
            tokenManager.clearToken()

            // Get new token
            val newToken = tokenManager.accessToken

            // Retry with new token
            val retryRequest = request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()

            response = chain.proceed(retryRequest)
        }

        return response
    }
}
