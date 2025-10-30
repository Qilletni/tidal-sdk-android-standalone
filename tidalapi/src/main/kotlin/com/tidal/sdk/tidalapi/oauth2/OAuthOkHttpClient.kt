package com.tidal.sdk.tidalapi.oauth2

import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.oltu.oauth2.client.HttpClient
import org.apache.oltu.oauth2.client.request.OAuthClientRequest
import org.apache.oltu.oauth2.client.response.OAuthClientResponse
import org.apache.oltu.oauth2.client.response.OAuthClientResponseFactory
import org.apache.oltu.oauth2.common.exception.OAuthProblemException
import org.apache.oltu.oauth2.common.exception.OAuthSystemException

/**
 * HTTP client adapter that bridges Apache Oltu OAuth2 library with OkHttp.
 * This implementation allows OAuth2TokenManager to use OkHttp for making token requests.
 */
class OAuthOkHttpClient : HttpClient {
    private val client: OkHttpClient

    /**
     * Creates a new OAuthOkHttpClient with a default OkHttpClient instance.
     */
    constructor() {
        this.client = OkHttpClient()
    }

    /**
     * Creates a new OAuthOkHttpClient with a custom OkHttpClient instance.
     *
     * @param client The OkHttpClient to use for making requests
     */
    constructor(client: OkHttpClient) {
        this.client = client
    }

    @Throws(OAuthSystemException::class, OAuthProblemException::class)
    override fun <T : OAuthClientResponse?> execute(
        request: OAuthClientRequest,
        headers: MutableMap<String?, String?>?,
        requestMethod: String?,
        responseClass: Class<T?>?
    ): T? {
        try {
            val builder = Request.Builder()
                .url(request.getLocationUri())

            // Add custom headers
            if (headers != null) {
                for (entry in headers.entries) {
                    builder.addHeader(entry.key!!, entry.value!!)
                }
            }

            // Build request with appropriate method and body
            if ("POST".equals(requestMethod, ignoreCase = true)) {
                val body = request.getBody()
                val requestBody: RequestBody = RequestBody.create(
                    "application/x-www-form-urlencoded".toMediaTypeOrNull(),
                    body ?: "",
                )
                builder.post(requestBody)
            } else if ("GET".equals(requestMethod, ignoreCase = true)) {
                builder.get()
            } else {
                throw OAuthSystemException("Unsupported HTTP method: $requestMethod")
            }

            // Execute request
            val response = client.newCall(builder.build()).execute()

            // Extract response body
            var responseBody = ""
            val body = response.body
            if (body != null) {
                responseBody = body.string()
            }

            // Convert to OAuthClientResponse
            return OAuthClientResponseFactory.createCustomResponse(
                responseBody,
                response.header("Content-Type"),
                response.code,
                response.headers.toMultimap(),
                responseClass,
            )
        } catch (e: IOException) {
            throw OAuthSystemException("HTTP request failed", e)
        }
    }

    override fun shutdown() {
        // Properly shutdown OkHttp resources to allow JVM to exit
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }
}
