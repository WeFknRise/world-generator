allprojects {
    group = "com.wefknrise"
    version = "1.0"
}

plugins {
    val kotlinVersion = "1.3.70"
    kotlin("multiplatform") version kotlinVersion apply false
    kotlin("js") version kotlinVersion apply false
    kotlin("plugin.spring") version kotlinVersion apply false
    kotlin("plugin.serialization") version kotlinVersion apply false
}