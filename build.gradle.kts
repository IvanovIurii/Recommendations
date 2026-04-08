import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.3.0"
    kotlin("plugin.spring") version "2.3.0"
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
    jacoco
}

group = "com.hse"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("io.awspring.cloud:spring-cloud-aws-dependencies:3.4.2")
        mavenBom("software.amazon.awssdk:bom:2.31.78")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core:11.20.1")
    implementation("org.flywaydb:flyway-database-postgresql:11.20.1")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.1")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.14")

    implementation("io.awspring.cloud:spring-cloud-aws-starter:3.4.2")
    implementation("io.awspring.cloud:spring-cloud-aws-autoconfigure:3.4.2")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sns:3.4.2")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs:3.4.2")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.14.7")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testImplementation("software.amazon.awssdk:sns")
    testImplementation("software.amazon.awssdk:sqs")
    testImplementation("org.wiremock:wiremock-standalone:3.12.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.withType<Test> {
    maxHeapSize = "2g"
    useJUnitPlatform()
    testLogging.events =
        setOf(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
        )
}

tasks.bootJar {
    archiveFileName.set("app.jar")
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}
