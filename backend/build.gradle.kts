import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    kotlin("plugin.serialization")
    id("org.springframework.boot") version "2.2.5.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

repositories {
    jcenter()
    mavenCentral()
    maven("https://repo.spring.io/milestone")
}

dependencies {
    val coroutinesVersion = "1.3.4"
    val serializationVersion = "0.20.0"
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion")
    implementation(project(":shared"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotlintest:kotlintest-runner-junit5:3.3.0")
    testImplementation("org.assertj:assertj-core:3.15.0")
}

tasks {
    withType < Test > {
    useJUnitPlatform()
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    processResources {
        dependsOn(":frontend:browserDevelopmentWebpack")
        from(project(":frontend").projectDir.resolve("src/main/resources")) {
            into("static")
        }
        from(project(":frontend").buildDir.resolve("distributions/frontend.js"))  {
            into("static")
        }
    }
}
