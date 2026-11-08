dependencies {
    testImplementation(project(":examples:telegram-boot-sample-app"))
    testImplementation(project(":telegram-boot-spring-boot-starter"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
