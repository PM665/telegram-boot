pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.spring.io/milestone")
    }
    plugins {
        kotlin("kapt") version "2.4.21"
    }
}

rootProject.name = "telegram-boot"
include(
    "telegram-boot-core",
    "telegram-boot-telegram",
    "telegram-boot-max",
    "telegram-boot-gmail",
    "telegram-boot-ai",
    "telegram-boot-autoconfigure",
    "telegram-boot-spring-data",
    "telegram-boot-spring-boot-starter",
    "telegram-boot-admin",
    "telegram-boot-e2e-tests",
    "examples:telegram-boot-sample-app",
)
