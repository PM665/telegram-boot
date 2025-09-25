plugins {
    `java-library`
}

dependencies {
    api(project(":telegram-boot-core"))
    implementation(project(":telegram-boot-autoconfigure"))
}
