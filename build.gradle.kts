import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.RunConfigurationContainer

plugins {
  id("java-library")
  id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.8"
  id("eclipse")
  id("com.gtnewhorizons.retrofuturagradle") version "1.4.0"
  id("com.diffplug.spotless") version "6.25.0"
}

group = "minefantasy.mf2.minefantasy2"
version = "2.8.14.7"

val mcVersion = "1.7.10"
val versionNEI = "2.7.72-GTNH"
val versionNotEnoughIds = "2.1.10"
val versionWaila = "1.8.12"
val versionCraftTweaker = "3.4.2"
val unimixinsVersion = "0.1.22"

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
    vendor.set(JvmVendorSpec.AZUL)
  }
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
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

val projVersion = project.version.toString()
tasks.processResources.configure {
  inputs.property("version", projVersion)
  inputs.property("mcversion", mcVersion)
  filesMatching("mcmod.info") {
    expand(
      mapOf(
        "version" to projVersion,
        "mcversion" to mcVersion
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
  maven {
    name = "JitPack"
    url = uri("https://jitpack.io")
    mavenContent {
      includeGroup("com.github.LegacyModdingMC.UniMixins")
      includeGroupByRegex("com\\.github\\..+")
    }
  }
  flatDir { dirs("lib") }
}

dependencies {
  api("com.github.GTNewHorizons:NotEnoughItems:${versionNEI}:dev")
  api("com.github.GTNewHorizons:NotEnoughIds:${versionNotEnoughIds}:dev")
  api("com.github.GTNewHorizons:waila:${versionWaila}:dev")
  compileOnly(files("lib/bukkit-1.7.10.jar"))

  compileOnly("com.github.GTNewHorizons:CraftTweaker:${versionCraftTweaker}:dev") {
    isTransitive = false
  }

  implementation("com.github.LegacyModdingMC.UniMixins:unimixins-mixin-1.7.10:$unimixinsVersion")
  //annotationProcessor("com.github.LegacyModdingMC.UniMixins:unimixins-mixin-1.7.10:$unimixinsVersion")

  constraints {
    implementation("org.apache.logging.log4j:log4j-core") {
      version {
        strictly("[2.17, 3[")
        prefer("2.19.0")
      }
      because(
        "CVE-2021-44228, CVE-2021-45046, CVE-2021-45105: Log4j vulnerable to remote code execution and other critical security vulnerabilities"
      )
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
    inheritOutputDirs = true
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

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
}

spotless {
  encoding("UTF-8")

  java {
    target("src/**/*.java")
    eclipse().configFile(rootProject.file("spotless.eclipseformat.xml"))
    importOrderFile(rootProject.file("spotless.importorder"))
    removeUnusedImports()
    trimTrailingWhitespace()
    endWithNewline()
  }
  format("misc") {
    target("*.md", ".gitattributes", ".gitignore")
    trimTrailingWhitespace()
    endWithNewline()
  }
  format("gradle") {
    target("**/*.gradle", "**/*.gradle.kts")
    trimTrailingWhitespace()
    endWithNewline()
  }
}

tasks.named("check").configure {
  dependsOn("spotlessCheck")
}

// Optional helper: install pre-commit hook to auto-run Spotless
tasks.register("installGitHook") {
  group = "formatting"
  description = "Installs a pre-commit git hook that runs spotlessApply"
  doLast {
    val hooksDir = file(".git/hooks")
    if (!hooksDir.exists()) hooksDir.mkdirs()
    val hookFile = file(".git/hooks/pre-commit")
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val script = if (isWindows) {
      """
      |@echo off
      |REM Run Gradle Spotless before commit
      |call gradlew.bat spotlessApply
      |if errorlevel 1 (
      |  echo Spotless formatting failed. Aborting commit.
      |  exit /b 1
      |)
      |exit /b 0
      |""".trimMargin().replace("\n", "\r\n")
    } else {
      """
      |#!/bin/sh
      |# Run Gradle Spotless before commit
      |./gradlew spotlessApply
      |if [ $? -ne 0 ]; then
      |  echo "Spotless formatting failed. Aborting commit."
      |  exit 1
      |fi
      |exit 0
      |""".trimMargin()
    }
    hookFile.writeText(script)
    if (!isWindows) hookFile.setExecutable(true)
    println("Pre-commit hook installed: ${hookFile}")
  }
}
