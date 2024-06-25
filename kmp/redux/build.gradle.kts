import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    `maven-publish`
    signing
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
        publishLibraryVariants("release", "debug")
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            group = "ch.smoca.lib"
            artifactId = "lightweight-kotlin-redux"
            version = "6.0.0"
            from(components["kotlin"])
            pom {
                name = "Lightweight Kotlin Redux"
                description = "A lightweight, kotlin multiplatform implementation of redux"
                url = "https://github.com/smoca-ag/lightweight-kotlin-redux"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
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
                    developerConnection =
                        "scm:git:ssh:///github.com:smoca-ag/lightweight-kotlin-redux.git"
                    url = "https://github.com/smoca-ag/lightweight-kotlin-redux"
                }
            }
        }
    }

    repositories {
        mavenCentral()
    }
}

signing {
    val keyId: String? by project
    val key: String? by project
    val pw: String? by project
    useInMemoryPgpKeys(keyId, key, pw)
    sign(publishing.publications["maven"])
}
