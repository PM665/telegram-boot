import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import org.gradle.api.publish.maven.MavenPublication

plugins {
    `java-library`
    id("com.vanniktech.maven.publish") version "0.34.0"
}

dependencies {
    api(project(":telegram-boot-core"))
    implementation(project(":telegram-boot-autoconfigure"))
}

mavenPublishing {
    coordinates(group.toString(), name, version.toString())

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
            }
        }
    }

    configure(JavaLibrary(
        javadocJar = JavadocJar.None(),
        sourcesJar = true,
    ))

    pom {
        name.set("Telegram-Boot Spring Boot Starter")
        description.set("This is the Spring Boot Starter for Telegram-Boot, a framework to build Telegram Bots in Java with Spring Boot.")
        inceptionYear.set("2025")
        url.set("https://github.com/pm665/telegram-boot/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("pm665")
                name.set("Pavel Minin")
                url.set("https://github.com/pm665/")
            }
        }
        scm {
            url.set("https://github.com/pm665/telegram-boot/")
            connection.set("scm:git:git://github.com/pm665/telegram-boot.git")
            developerConnection.set("scm:git:ssh://git@github.com/pm665/telegram-boot.git")
        }
    }
}
