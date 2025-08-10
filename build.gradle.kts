import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.RunConfigurationContainer

plugins {
  id("java-library")
  id("maven-publish")
  id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.8"
  id("eclipse")
  id("com.gtnewhorizons.retrofuturagradle") version "1.4.0"
}

group = "minefantasy.mf2.minefantasy2"
version = "2.8.14.7"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
    vendor.set(JvmVendorSpec.AZUL)
  }
  withSourcesJar()
  withJavadocJar()
}

minecraft {
  username.set(System.getProperty("user.name"))
  injectedTags.put("VERSION", project.version)
  extraRunJvmArguments.add("-ea:${project.group}")
}

tasks.injectTags.configure {
  outputClassName.set("${project.group}.Tags")
}

// mcmod.info version replacement
val projVersion = project.version.toString()
tasks.processResources.configure {
  // Ensure mcmod.info gets version and mcversion replaced
  inputs.property("version", projVersion)
  inputs.property("mcversion", "1.7.10")
  filesMatching("mcmod.info") {
    expand(
      mapOf(
        "version" to projVersion,
        "mcversion" to "1.7.10"
      )
    )
  }
}

val runtimeOnlyNonPublishable: Configuration by configurations.creating {
  description = "Runtime only dependencies that are not published alongside the jar"
  isCanBeConsumed = false
  isCanBeResolved = false
}
listOf(configurations.runtimeClasspath, configurations.testRuntimeClasspath).forEach {
  it.configure {
    extendsFrom(runtimeOnlyNonPublishable)
  }
}

repositories {
  maven {
    name = "OvermindDL1 Maven"
    url = uri("https://gregtech.overminddl1.com/")
  }
  maven {
    name = "GTNH Maven"
    url = uri("https://nexus.gtnewhorizons.com/repository/public/")
  }
  flatDir { dirs("lib") }
}

dependencies {
  api("com.github.GTNewHorizons:NotEnoughItems:2.7.72-GTNH:dev")
  api("com.github.GTNewHorizons:NotEnoughIds:2.1.10:dev")

  // Keep dev-only mod deps off the published POM
  compileOnly(files("lib/bukkit-1.7.10.jar"))

  compileOnly("com.github.GTNewHorizons:CraftTweaker:3.4.2:dev") {
    isTransitive = false
  }
}

publishing {
  publications {
    create<MavenPublication>("maven") {
      from(components["java"])
    }
  }
}

eclipse {
  classpath {
    isDownloadSources = true
    isDownloadJavadoc = true
  }
}

idea {
  module {
    isDownloadJavadoc = true
    isDownloadSources = true
    inheritOutputDirs = true // Fix resources in IJ-Native runs
  }
  project {
    this.withGroovyBuilder {
      "settings" {
        "runConfigurations" {
          val self = this.delegate as RunConfigurationContainer
          self.add(Gradle("1. Run Client").apply {
            setProperty("taskNames", listOf("runClient"))
          })
          self.add(Gradle("2. Run Server").apply {
            setProperty("taskNames", listOf("runServer"))
          })
          self.add(Gradle("3. Run Obfuscated Client").apply {
            setProperty("taskNames", listOf("runObfClient"))
          })
          self.add(Gradle("4. Run Obfuscated Server").apply {
            setProperty("taskNames", listOf("runObfServer"))
          })
        }
        "compiler" {
          val self = this.delegate as org.jetbrains.gradle.ext.IdeaCompilerConfiguration
          afterEvaluate {
            self.javac.moduleJavacAdditionalOptions = mapOf(
              (project.name + ".main") to
                tasks.compileJava.get().options.compilerArgs.joinToString(" ") { '"' + it + '"' }
            )
          }
        }
      }
    }
  }
}

tasks.processIdeaSettings.configure {
  dependsOn(tasks.injectTags)
}

tasks.withType<Javadoc>().configureEach {
  isFailOnError = false
  val opts = options as StandardJavadocDocletOptions
  opts.addStringOption("Xdoclint:none", "-quiet")
  opts.encoding = "UTF-8"
  opts.charSet = "UTF-8"
}
