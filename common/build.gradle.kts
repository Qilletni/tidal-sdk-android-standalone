plugins {
    kotlin("jvm")
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

dependencies {
    api(libs.kotlin.logging)
    api(libs.slf4j.api)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "common-standalone"
            groupId = "com.tidal.sdk"
            from(components["java"])
        }
    }
}
