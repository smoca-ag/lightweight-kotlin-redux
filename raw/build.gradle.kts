plugins {
    kotlin("jvm") version "1.9.23"
}

group = "ch.smoca"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

sourceSets {
    main {
        kotlin {
            srcDirs("../src/")
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
}

kotlin {
    jvmToolchain(17)
}