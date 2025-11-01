/**
 * TIDAL SDK API Module
 *
 * Provides access to the TIDAL Open API, including authentication, API endpoints,
 * and data models for interacting with TIDAL services.
 */
module com.tidal.sdk.tidalapi {
    // Export all public API packages
    exports com.tidal.sdk.tidalapi;
    exports com.tidal.sdk.tidalapi.oauth2;
    exports com.tidal.sdk.tidalapi.networking;
    exports com.tidal.sdk.tidalapi.generated;
    exports com.tidal.sdk.tidalapi.generated.apis;
    exports com.tidal.sdk.tidalapi.generated.models;

    // Common SDK module (transitive - part of public API)
    requires transitive com.tidal.sdk.common;

    // Kotlin dependencies
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;
    requires transitive kotlinx.serialization.json;
    requires transitive kotlinx.serialization.core;

    // Networking dependencies (transitive - part of public API)
    requires transitive retrofit2;
    requires transitive okhttp3;

    // Logging (inherited from common, but may be needed)
    requires org.slf4j;
}
