plugins {
    `java-library`
}

dependencies {
    api(project(":telegram-boot-core"))
    api(project(":telegram-boot-telegram"))
    api(project(":telegram-boot-max"))
    api(project(":telegram-boot-admin"))
    api(project(":telegram-boot-gmail"))
    api(project(":telegram-boot-autoconfigure"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
