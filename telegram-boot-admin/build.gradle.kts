plugins {
    `java-library`
}

dependencies {
    implementation(libs.bundles.telegram)
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-jackson2")
    implementation(libs.htmx.spring.boot.thymeleaf)
    implementation(libs.ngrok.spring.boot)
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation(project(":telegram-boot-core"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
