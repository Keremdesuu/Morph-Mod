plugins {
    id("fabric-loom") version "1.9.2"
    id("java")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")
}

loom {
    splitEnvironmentSourceSets()
    mods {
        register("morphmod") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
