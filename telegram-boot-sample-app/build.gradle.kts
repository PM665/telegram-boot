plugins {
    id("org.springframework.boot") version "3.5.0-SNAPSHOT"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(project(":telegram-boot-spring-boot-starter"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
