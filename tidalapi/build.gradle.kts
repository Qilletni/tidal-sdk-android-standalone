plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.plugin.serialization)
    id("maven-publish")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(21)
}

// Configure Java compiler to work with module-info.java and Kotlin sources
tasks {
    compileJava {
        // Compile Kotlin first, then Java (module-info.java)
        dependsOn(compileKotlin)
        // Patch the module with Kotlin-compiled classes so Java compiler can see them
        options.compilerArgs.addAll(listOf(
            "--patch-module",
            "com.tidal.sdk.tidalapi=${compileKotlin.get().destinationDirectory.asFile.get().path}"
        ))
    }
}

dependencies {
    api(project(":common"))

    api(libs.kotlinx.serialization.json)
    api(libs.retrofit)

    implementation(libs.kotlinxCoroutinesCore)
    implementation(libs.okhttp.loggingInterceptor)
    implementation(libs.converter.kotlinx.serialization)
    implementation(libs.converter.scalars)

    // Apache Oltu OAuth2 dependencies for OAuth2TokenManager
    implementation("org.apache.oltu.oauth2:org.apache.oltu.oauth2.client:1.0.2")
    implementation("org.apache.oltu.oauth2:org.apache.oltu.oauth2.common:1.0.2")

    testImplementation(libs.test.junit5Api)
    testRuntimeOnly(libs.test.junit5Engine)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "tidalapi-standalone"
            groupId = "com.tidal.sdk"
            from(components["java"])
        }
    }
}
