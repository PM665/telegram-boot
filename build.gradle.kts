import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spotless)
}

val springBootVersion = libs.versions.springBoot.get()
val springBootBom = "org.springframework.boot:spring-boot-dependencies:$springBootVersion"
val ktlintVersion = libs.versions.ktlint.get()
// Preserve ktlint 1.2.1 layout after the engine upgrade; remove only with an intentional repo-wide reformat.
val ktlintCompatibilityOverrides =
    mapOf(
        "ktlint_standard_argument-list-wrapping" to "disabled",
        "ktlint_standard_binary-expression-wrapping" to "disabled",
        "ktlint_standard_blank-line-between-when-conditions" to "disabled",
        "ktlint_standard_chain-method-continuation" to "disabled",
        "ktlint_standard_function-literal" to "disabled",
        "ktlint_standard_function-signature" to "disabled",
        "ktlint_standard_indent" to "disabled",
        "ktlint_standard_max-line-length" to "disabled",
        "ktlint_standard_multiline-expression-wrapping" to "disabled",
        "ktlint_standard_parameter-list-wrapping" to "disabled",
        "ktlint_standard_string-template-indent" to "disabled",
        "ktlint_standard_trailing-comma-on-call-site" to "disabled",
        "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
        "ktlint_standard_when-entry-bracing" to "disabled",
        "ktlint_standard_wrapping" to "disabled",
    )

allprojects {
    group = "io.github.pm665"
    version = "0.2.34"

    repositories {
        mavenCentral()
    }
}

spotless {
    kotlinGradle {
        target("*.gradle.kts", "settings.gradle.kts", "gradle/**/*.gradle.kts")
        ktlint(ktlintVersion).editorConfigOverride(ktlintCompatibilityOverrides)
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")
    apply(plugin = "org.jetbrains.kotlin.plugin.spring")
    apply(plugin = "com.diffplug.spotless")

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(25)
            compilerOptions.jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
    }

    dependencies {
        add("implementation", enforcedPlatform(springBootBom))
        listOf("annotationProcessor", "kapt", "testAnnotationProcessor", "kaptTest").forEach {
            add(it, enforcedPlatform(springBootBom))
        }
    }

    pluginManager.withPlugin("org.springframework.boot") {
        dependencies {
            add("developmentOnly", enforcedPlatform(springBootBom))
        }
    }

    spotless {
        kotlin {
            target("src/**/*.kt")
            ktlint(ktlintVersion).editorConfigOverride(ktlintCompatibilityOverrides)
        }
        kotlinGradle {
            target("*.gradle.kts", "src/**/*.gradle.kts")
            ktlint(ktlintVersion).editorConfigOverride(ktlintCompatibilityOverrides)
        }

        java {
            removeUnusedImports()
            palantirJavaFormat()
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
