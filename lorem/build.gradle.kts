import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.github.dnedrow"
version = "0.1.0"

kotlin {
    explicitApi()

    jvm()
    androidLibrary {
        namespace = "io.github.dnedrow.lorem"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}.configure {}

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "lorem", version.toString())

    pom {
        name = "Lorem"
        description = "Deterministic, seeded Lorem Ipsum generation for Kotlin Multiplatform."
        inceptionYear = "2026"
        // TODO: Publishing destination is undecided; see design.md "Migration Plan".
        // Complete url, developers, and scm before the first release.
        url = "TBD"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "TBD"
                name = "TBD"
                url = "TBD"
            }
        }
        scm {
            url = "TBD"
            connection = "TBD"
            developerConnection = "TBD"
        }
    }
}
