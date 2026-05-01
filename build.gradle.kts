plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "com.example"
version = "1.6-SNAPSHOT"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.3.1")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf(/* Plugin Dependencies */))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    processResources {
        exclude("BongoCat_sound/BongoSound/Bongo 1 by coldwellw.wav")
        exclude("BongoCat_sound/KeyboardSound/Keyboard Sounds Pack by Haratman.wav")
    }

    patchPluginXml {
        untilBuild.set(provider { null })
        changeNotes.set("""
          <ul>
              <li>Added Bongo and Keyboard sound modes.</li>
              <li>Added persistent sound mode setting.</li>
              <li>Improved Korean IME input handling to prevent duplicate sounds.</li>
              <li>Added low-latency WAV playback.</li>
          </ul>
      """.trimIndent())
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
