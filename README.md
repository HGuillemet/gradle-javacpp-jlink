## JavaCPP jlink plugin ##

JavaCPP presets are distributed with native libraries bundled in jar files.
Images of modularized applications using JavaCPP can be built with jlink by
including the native jar files in the image, since they are named modules.
However, it is not the best solution for the following reasons:

* the native library will be silently extracted to a user cache directory
  (`$HOME/.javacpp/cache` if not configured otherwise) that is never cleaned.
* the native jar may contain a lot of useless libraries and other files you don't want to include in your minimal image.
* the native modules must be added in the modules graph using `--add-modules` command line option of jlink and this may
  be tricky.

This Gradle plugin overcomes these problems by pre-extracting the actually-used libraries in the
Java runtime of the jlink image and thus allowing to not include the native jar file in the image.
It automatically applies two plugins: the Badass jlink plugin and JavaCPP libextract plugin.

More precisely, this plugin does the following:

* A task called `libExtractToImage`, of type `ExtractLibraries` from the libextract plugin,
  is run as a finalizer of the `jlink` task to extract the libraries used by the `main` source set and
  by all launchers in the runtime image, in the proper subdirectory depending on the target platform.

* The Badass jlink plugin extension is amended before execution of the `jlink` task by adding command line parameter
  `-Dorg.bytedeco.javacpp.findlibraries=false` to the launchers.
  The `findLibraries` property, added in JavaCPP 1.5.8, inform JavaCPP to skip the usual library search in resources.

* Linux and MacOSX: until OpenJDK 22
  (see [Bug #8310933](https://bugs.openjdk.org/browse/JDK-8310933)), when jpackage
  copies a jlink custom runtime to an application image, symbolic links are followed. Since JavaCPP creates symlinks when
  extracting native libraries, we end up storing multiple
  copies of the same native libraries in the application packages. This
  plugin works around this by adding an action to the `jpackageImage` task of the Badass jlink plugin to
  restore the symbolic links. Note that if you asked jpackage to sign the app on MacOSX, the
  replacement of the native libraries by symlinks will invalidate the signature. You need to sign the app again,
  for instance in a task registered as finalizer for `jpackageImage`.

The build script of an example application using OpenCV is shown below. To build the image of such application, run:

```bash
./gradlew jlink --no-daemon
```

or

```bash
./gradlew jpackage --no-daemon
```

Groovy DSL:

```groovy
plugins {
    id 'fr.apteryx.javacpp-jlink' version '0.6'
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.bytedeco:opencv-platform:4.7.0-1.5.9'
}

group = 'my.group'
version = '1.0'
description = 'my-description'

application {
    mainModule = 'my.module'
    mainClass = 'my.module.MainClass'
}

jlink {
    jpackage {
        vendor = 'my institution'
    }
}
```

Kotlin DSL:

```kotlin
plugins {
    id("fr.apteryx.javacpp-jlink") version "0.6"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.bytedeco:opencv-platform:4.7.0-1.5.9")
}

group = "my.group"
version = "1.0"
description = "my-description"

application {
    mainModule.set("my.module")
    mainClass.set("my.module.MainClass")
}

jlink {
    jpackage {
        vendor = "my institution"
    }
}
```

Users must be aware of these limitations:

1. Cross-platform jlink is not supported. An exception is thrown if the Badass jlink plugin is configured with specific
   target platforms.
2. Supported operating systems are macOS, Linux and Windows.
3. This plugin cannot run in Gradle daemon. Daemon mode can be disabled using the `--no-daemon` Gradle command line
   option, or by adding `org.gradle.daemon=false` to your `gradle.properties` file.