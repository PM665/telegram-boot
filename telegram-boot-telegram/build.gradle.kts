plugins {
    `java-library`
}

dependencies {
    api(project(":telegram-boot-core"))
    implementation(libs.bundles.telegram)
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")
    implementation("io.micrometer:context-propagation")
    implementation(libs.bundles.observability)
    implementation("io.github.resilience4j:resilience4j-spring-boot4:2.4.0")
    implementation(kotlin("reflect"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.mockito.kotlin)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(kotlin("test"))
}
