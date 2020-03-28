plugins {
    kotlin("js")
}

repositories {
    mavenCentral()
}

kotlin {
    target {
        browser()

        sourceSets {
            main {
                dependencies {
                    val coroutinesVersion = "1.3.4"
                    val serializationVersion = "0.20.0"
                    implementation(kotlin("stdlib-js"))
                    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serializationVersion")
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
                    implementation(project(":shared"))
                }
            }
        }
    }
}