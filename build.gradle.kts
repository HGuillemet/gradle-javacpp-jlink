plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "0.18.0"
}

pluginBundle {
    website = "https://github.com/HGuillemet/gradle-javacpp-jlink"
    vcsUrl = "https://github.com/HGuillemet/gradle-javacpp-jlink.git"
    tags = listOf("javacpp", "jlink", "jpms")
}
gradlePlugin {
    plugins {
        create("jlink") {
            id = "fr.apteryx.javacpp-jlink"
            displayName = "JavaCPP jlink plugin"
            description = "Create a jlink image for applications using JavaCPP"
            implementationClass = "fr.apteryx.gradle.javacpp.jlink.Plugin"
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}

group = "fr.apteryx"
version = "0.2"

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation("fr.apteryx:gradle-javacpp-libextract:0.2")
    implementation("org.beryx:badass-jlink-plugin:2.25.0")
}
