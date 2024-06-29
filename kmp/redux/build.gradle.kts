import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("com.vanniktech.maven.publish") version "0.29.0"
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "library"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            kotlin {
                srcDirs("../../src")
            }
            dependencies {
                implementation(libs.kotlin.coroutines.core)
            }
        }
        commonTest {
            kotlin {
                srcDirs("../../tests")
            }
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test:2.0.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
            }
        }


    }
}

android {
    namespace = "ch.smoca.redux"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

mavenPublishing {
    coordinates(
        groupId = "ch.smoca.lib",
        artifactId = "lightweight-kotlin-redux",
        version = "6.1.0"
    )
    configure(
        KotlinMultiplatform(
            // - `JavadocJar.None()` don't publish this artifact
            // - `JavadocJar.Empty()` publish an empty jar
            // - `JavadocJar.Dokka("dokkaHtml")` when using Kotlin with Dokka, where `dokkaHtml` is the name of the Dokka task that should be used as input
            javadocJar = JavadocJar.Empty(),
            // whether to publish a sources jar
            sourcesJar = true,
            // configure which Android library variants to publish if this project has an Android target
            androidVariantsToPublish = listOf("debug", "release"),
        )
    )
    pom {
        name = "Lightweight Kotlin Redux"
        description = "A lightweight, kotlin multiplatform implementation of redux"
        inceptionYear = "2024"
        url = "https://github.com/smoca-ag/lightweight-kotlin-redux"
        licenses {
            license {
                name = "MIT"
                url =
                    "https://github.com/smoca-ag/lightweight-kotlin-redux?tab=readme-ov-file#license"
            }
        }
        developers {
            developer {
                id = "smoca-ag"
                name = "Smoca AG"
                email = "info@smoca.ch"
                organization = "Smoca AG"
                organizationUrl = "https://smoca.ch"
            }
        }
        scm {
            connection = "scm:git:git://github.com:smoca-ag/lightweight-kotlin-redux.git"
            developerConnection = "scm:git:ssh:///github.com:smoca-ag/lightweight-kotlin-redux.git"
            url = "https://github.com/smoca-ag/lightweight-kotlin-redux"
        }
    }

    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}
