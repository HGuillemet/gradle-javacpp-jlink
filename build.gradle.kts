plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "0.21.0"
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

group = "fr.apteryx"
version = "0.1"

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation("fr.apteryx:gradle-javacpp-libextract:0.1")
    implementation("org.beryx:badass-jlink-plugin:2.25.0")
}