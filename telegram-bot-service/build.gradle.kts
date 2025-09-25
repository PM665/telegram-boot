plugins {
    `java-library`
}

dependencies {
    val springBootVersion: String by rootProject.extra
    val lombokVersion: String by rootProject.extra

    api("org.springframework.boot:spring-boot")

    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
