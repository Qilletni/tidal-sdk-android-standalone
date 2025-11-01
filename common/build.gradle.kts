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

// Configure Java compiler to work with module-info.java and Kotlin sources
tasks {
    compileJava {
        // Compile Kotlin first, then Java (module-info.java)
        dependsOn(compileKotlin)
        // Patch the module with Kotlin-compiled classes so Java compiler can see them
        options.compilerArgs.addAll(listOf(
            "--patch-module",
            "com.tidal.sdk.common=${compileKotlin.get().destinationDirectory.asFile.get().path}"
        ))
    }
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
