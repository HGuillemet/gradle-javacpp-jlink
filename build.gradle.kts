plugins {
    id("com.gradle.plugin-publish") version "1.2.0"
}

group = "fr.apteryx"
version = "0.6"

gradlePlugin {
    website.set("https://github.com/HGuillemet/gradle-javacpp-jlink")
    vcsUrl.set("https://github.com/HGuillemet/gradle-javacpp-jlink.git")
    plugins {
        create("jlink") {
            id = "fr.apteryx.javacpp-jlink"
            displayName = "JavaCPP jlink plugin"
            description = "Create a jlink image for applications using JavaCPP"
            tags.set(listOf("javacpp", "jlink", "jpms"))
            implementationClass = "fr.apteryx.gradle.javacpp.jlink.Plugin"
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation("fr.apteryx:gradle-javacpp-libextract:0.6")
    implementation("org.beryx:badass-jlink-plugin:3.0.1")
}
