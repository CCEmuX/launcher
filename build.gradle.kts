plugins {
	application
	id("org.jetbrains.kotlin.jvm") version "1.8.0"
	id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "cc.emux"
version = "1.0"

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-stdlib")
}

application {
	mainClass.set("cc.emux.launcher.Launcher")
}

tasks.compileKotlin { kotlinOptions.jvmTarget = "1.8" }
tasks.compileTestKotlin { kotlinOptions.jvmTarget = "1.8" }

tasks.shadowJar {
	minimize()
	exclude("**/*.kotlin_metadata")
}
