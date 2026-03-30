plugins {
    kotlin("jvm") version "1.9.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"  // опционально, для fat JAR
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.bytedeco:javacv-platform:1.5.9")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}

application {
    mainClass.set("ImageTextPreprocessorKt")
}