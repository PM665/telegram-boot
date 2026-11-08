plugins {
    id("org.springframework.boot") version "4.1.2"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation(project(":telegram-boot-spring-boot-starter"))
    implementation(kotlin("reflect"))
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.bootRun {
    workingDir = projectDir
}
