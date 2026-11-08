plugins {
    `java-library`
    kotlin("kapt")
}

dependencies {
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("io.micrometer:context-propagation")
    implementation(libs.bundles.observability)
    implementation(libs.bundles.observabilityStack)
    implementation("io.github.resilience4j:resilience4j-spring-boot4:2.4.0")
    implementation("org.springframework.boot:spring-boot-starter-mustache")

    implementation(kotlin("reflect"))
    implementation("org.slf4j:slf4j-api")

    kapt("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(libs.mockito.kotlin)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(kotlin("test"))
}
repositories {
    mavenCentral()
}
