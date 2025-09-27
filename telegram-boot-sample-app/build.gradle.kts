val springBootVersion: String by rootProject.extra

plugins {
    id("org.springframework.boot") version "3.5.6"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(project(":telegram-boot-spring-boot-starter"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
