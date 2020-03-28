plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

repositories {
    mavenCentral()
    jcenter()
}

kotlin {
    mingwX64()
    jvm()
    js {
        browser()
        nodejs()
    }

    sourceSets {
        val serializationVersion = "0.20.0"
        commonMain {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serializationVersion")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        getByName("jvmMain").apply {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
            }
        }
        getByName("jvmTest").apply {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        getByName("jsMain").apply {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion")
            }
        }
        getByName("jsTest").apply {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}