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
    "telegram-boot-core",
    "telegram-boot-autoconfigure",
    "telegram-boot-spring-boot-starter",
    "telegram-boot-sample-app"
)
