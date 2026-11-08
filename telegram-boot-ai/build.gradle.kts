plugins {
    `java-library`
}

dependencies {
    api(platform(libs.spring.ai.bom))
    api(libs.spring.ai.model)
    implementation(libs.spring.ai.ollama.starter)
    implementation(libs.spring.ai.openai.starter)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
