plugins {
	id("net.fabricmc.fabric-loom-remap")
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

base {
	archivesName = providers.gradleProperty("archives_base_name")
}

repositories {
	maven {
		name = "Fabric"
		url = uri("https://maven.fabricmc.net/")
	}
	mavenCentral()
}

loom {
	runs {
		named("client") {
			programArgs("--username", "Player77")
		}
	}
}

dependencies {
	minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
	mappings("net.fabricmc:yarn:${providers.gradleProperty("yarn_mappings").get()}:v2")
	modImplementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")
	modImplementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
}

tasks.processResources {
	inputs.property("version", version)
	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.release = 21
}

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.jar {
	from("LICENSE") {
		rename { "${it}_${base.archivesName.get()}" }
	}
}
