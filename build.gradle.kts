import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    java
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.0.21" apply false
    kotlin("kapt") version "2.0.21" apply false
    kotlin("plugin.spring") version "2.0.21" apply false
    id("com.diffplug.spotless") version "6.25.0"
}

val springBootVersion by extra("3.5.6")

allprojects {
    group = "io.github.pm665"
    version = "0.0.1"

    repositories {
        mavenCentral()
    }
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "settings.gradle.kts", "gradle/**/*.gradle.kts")
        ktlint("1.2.1")
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "com.diffplug.spotless")
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(21)
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    configure<DependencyManagementExtension> {
        val springBootVersion: String by rootProject.extra
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        }
    }

    spotless {
        kotlin {
            target("src/**/*.kt")
            ktlint("1.2.1")
        }
        kotlinGradle {
            target("*.gradle.kts", "src/**/*.gradle.kts")
            ktlint("1.2.1")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events = setOf(TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.PASSED)
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    tasks.named("check") {
        dependsOn("spotlessCheck")
    }

    tasks.named("build") {
        dependsOn("spotlessApply")
    }

    tasks.matching { it.name == "compileKotlin" || it.name == "compileJava" }.configureEach {
        dependsOn("spotlessApply")
    }
}

tasks.named("build") {
    dependsOn("spotlessApply")
}
