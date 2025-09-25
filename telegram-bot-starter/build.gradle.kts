plugins {
    `java-library`
}

dependencies {
    api(project(":telegram-bot-service"))
    implementation(project(":telegram-bot-autoconfigure"))
}
