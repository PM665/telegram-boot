pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.spring.io/milestone")
        maven("https://repo.spring.io/snapshot")
    }
}

rootProject.name = "telegram-boot"
include(
    "telegram-bot-service",
    "telegram-bot-autoconfigure",
    "telegram-bot-starter",
    "telegram-app"
)
