package com.tidal.sdk.tidalapi.networking

import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import okhttp3.Interceptor
import okhttp3.Response

/**
 * This fixes a potential bug in Tidal's API wrapper where parentheses in searches are
 * improperly encoded, causing bogus results. I have not yet tested this on the actual Tidal
 * Android SDK, but this interceptor works.
 */
class StrictEncodingInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url

        // Get the decoded path segments
        val pathSegments: List<String> = url.pathSegments

        // Currently, only doing this special encoding on /searchResults/
        if (!pathSegments.contains("searchResults")) {
            return chain.proceed(request)
        }

        // Start a new builder and clear the existing path
        // encodedPath("/") effectively resets the path so we can rebuild it segment by segment
        val newUrlBuilder = url.newBuilder().encodedPath("/")

        for (i in pathSegments.indices) {
            val segment = pathSegments[i]

            // Get query from /searchResults/{query}
            val isSearchQuery = i > 0 && "searchResults" == pathSegments[i - 1]

            if (isSearchQuery) {
                // Apply strict encoding
                // URLEncoder encodes '(', ')', and ''' which standard OkHttp leaves alone
                val strictEncoded = URLEncoder.encode(segment, StandardCharsets.UTF_8)
                    .replace("+", "%20") // Fix space encoding

                // Add as an encoded path segment to prevent OkHttp from re-encoding the % symbols
                newUrlBuilder.addEncodedPathSegment(strictEncoded)
            } else {
                // For normal segments let OkHttp handle standard encoding
                newUrlBuilder.addPathSegment(segment)
            }
        }

        val newRequest = request.newBuilder()
            .url(newUrlBuilder.build())
            .build()

        return chain.proceed(newRequest)
    }
}
