import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom")
	id("org.jetbrains.kotlin.jvm") version "2.4.10"

	id("com.modrinth.minotaur") version "2.+"
}

repositories {
	mavenCentral()
	maven {
		name = "repo.pauli.fyiReleases"
		url = uri("https://repo.pauli.fyi/releases")
	}
}

loom {
	splitEnvironmentSourceSets()

	mods {
		register("reallybouncyballs") {
			sourceSet(sourceSets.main.get())
			sourceSet(sourceSets.getByName("client"))
		}
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

	implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    implementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

	/*val silkVersion = "1.12.0"
	println("Silk: $silkVersion")
	implementation("net.silkmc:silk-core:$silkVersion")
	implementation("net.silkmc:silk-commands:$silkVersion")
	implementation("net.silkmc:silk-nbt:$silkVersion")
	implementation("net.silkmc:silk-network:$silkVersion")*/
}

tasks.processResources {
	val version = version
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = 25
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_25
	}
}

java {
	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

modrinth {
	token = System.getenv("MODRINTH_TOKEN")
	projectId = "reallybouncyballs"
	versionNumber = version.toString()
	versionType = "release"
	uploadFile.set(tasks.jar)
	gameVersions.addAll(buildList {
		add(providers.gradleProperty("minecraft_version").get())
	})
	loaders.addAll(buildList {
		add("fabric")
	})
	dependencies {
		required.project("fabric-api")
		required.project("fabric-language-kotlin")
	}
}