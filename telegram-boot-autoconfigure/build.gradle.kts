plugins {
    `java-library`
}

dependencies {
    implementation(project(":telegram-boot-core"))
    implementation(project(":telegram-boot-admin"))
    compileOnly(project(":telegram-boot-spring-data"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(kotlin("reflect"))

    kapt("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":telegram-boot-spring-data"))
    testImplementation("org.springframework.data:spring-data-commons")
    testImplementation(libs.micrometer.core)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
