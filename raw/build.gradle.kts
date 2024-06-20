/*
 this can not build on its own, only when imported as module in an android project
 the android project must implement coroutines core:

 [libs.versions.toml]
 kotlin-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }

 */

plugins {
    kotlin("jvm")
}

group = "ch.smoca"

sourceSets {
    main {
        kotlin {
            srcDirs("../src/")
        }
    }
}

dependencies {
    implementation(libs.kotlin.coroutines.core)
}
