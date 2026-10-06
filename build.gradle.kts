import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.RunConfigurationContainer

plugins {
  id("java-library")
  id("org.jetbrains.gradle.plugin.idea-ext") version "1.4.1"
  id("eclipse")
  id("com.gtnewhorizons.retrofuturagradle") version "2.0.4"
  id("com.diffplug.spotless") version "6.25.0"
}

group = "minefantasy.mf2.minefantasy2"
version = "2.8.15.3"

val mcVersion = "1.7.10"
val versionNEI = "2.8.102-GTNH"
val versionWaila = "1.19.32"
val versionCraftTweaker = "3.4.8"
val versionBattlegear = "1.6.8-backhand"
val versionThaumcraft = "1.7.10-4.2.3.5"
val versionBaubles = "1.0.1.10"
val versionHorizonQA = "0.15.0"
java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(8))
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
listOf(configurations.runtimeClasspath).forEach {
  it.configure {
    extendsFrom(runtimeOnlyNonPublishable)
  }
}

// Game tests (Horizon-QA): their own source set, never in the mod jar, loaded by runServer alongside the mod
val gameTestMods: Configuration by configurations.creating {
  description = "Mods the game tests need on the development server, never published"
  isCanBeConsumed = false
}
val gameTest: SourceSet = sourceSets.create("gameTest") {
  compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath + gameTestMods
  runtimeClasspath += output + compileClasspath
}

repositories {
  mavenCentral()
  maven {
    name = "GTNH Maven"
    url = uri("https://nexus.gtnewhorizons.com/repository/public/")
  }
  maven {
    name = "OvermindDL1 Maven"
    url = uri("https://gregtech.overminddl1.com/")
  }
  maven {
    name = "JitPack"
    url = uri("https://jitpack.io")
    mavenContent {
      includeGroup("com.github.LegacyModdingMC.UniMixins")
      includeGroupByRegex("com\\.github\\..+")
    }
  }
}

dependencies {
  api("com.github.GTNewHorizons:NotEnoughItems:${versionNEI}:dev")
  // Kept for the Waila support still to be written; nothing references it yet. Not transitive: it drags in
  // cofh-core from the CurseForge repository, which this build does not declare, and its own older NEI.
  api("com.github.GTNewHorizons:waila:${versionWaila}:dev") {
    isTransitive = false
  }

  compileOnly("com.github.GTNewHorizons:CraftTweaker:${versionCraftTweaker}:dev") {
    isTransitive = false
  }
  // Game tests run on the development server only: Horizon-QA, and CraftTweaker for the script adapters
  gameTestMods("com.github.GTNewHorizons:Horizon-QA:${versionHorizonQA}:dev") {
    isTransitive = false
  }
  gameTestMods("com.github.GTNewHorizons:CraftTweaker:${versionCraftTweaker}:dev") {
    isTransitive = false
  }

  // Soft dependency: every call into Battlegear is guarded by Loader.isModLoaded("battlegear2"), and the
  // interfaces the items expose to it are stripped by @Optional.Interface when the mod is absent.
  compileOnly("com.github.GTNewHorizons:Battlegear2-for-Backhand:${versionBattlegear}:dev") {
    isTransitive = false
  }
  // Present in runClient and runServer so the integration can actually be exercised, but kept off the published
  // metadata because the mod stays optional. Transitive here: it needs Backhand to load at all.
  runtimeOnlyNonPublishable("com.github.GTNewHorizons:Battlegear2-for-Backhand:${versionBattlegear}:dev")

  // Soft dependency, compile only: the API classes must not end up in the published jar, and every call is
  // guarded by Loader.isModLoaded("Thaumcraft"). Not transitive: the dev jar lists the whole Thaumcraft
  // runtime, none of which this build needs to compile against.
  compileOnly("thaumcraft:Thaumcraft:${versionThaumcraft}:dev") {
    isTransitive = false
  }
  // Development and testing only, never published: without the full mod in runClient and runServer the guarded
  // branches of TCCompat cannot be exercised at all. Baubles is Thaumcraft's own required dependency.
  runtimeOnlyNonPublishable("thaumcraft:Thaumcraft:${versionThaumcraft}:dev") {
    isTransitive = false
  }
  runtimeOnlyNonPublishable("com.azanor.baubles:Baubles:${versionBaubles}:deobf") {
    isTransitive = false
  }

  constraints {
    implementation("org.apache.logging.log4j:log4j-api") {
      version {
        strictly("2.0-beta9-fixed")
      }
      because("Forge 1.7.10/FML cannot scan modern multi-release Log4j jars during mod discovery")
    }
    implementation("org.apache.logging.log4j:log4j-core") {
      version {
        strictly("2.0-beta9-fixed")
      }
      because("Forge 1.7.10/FML cannot scan modern multi-release Log4j jars during mod discovery")
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
  // Enforce LF line endings across all Spotless formats to avoid CRLF noise and hook shebang issues
  lineEndings = com.diffplug.spotless.LineEnding.UNIX

  java {
    target("src/**/*.java")
    targetExclude("build/**", "run/**", "**/generated/**")
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
    target("*.gradle", "*.gradle.kts", "gradle/**/*.gradle", "gradle/**/*.gradle.kts")

    targetExclude("build/**", "run/**", ".gradle/**")
    trimTrailingWhitespace()
    endWithNewline()
  }
}

tasks.named("check").configure {
  dependsOn("spotlessCheck")
}

tasks.findByName("extractNatives2")?.let { extractTask ->
  tasks.named("spotlessGradle").configure { mustRunAfter(extractTask) }
  tasks.named("spotlessGradleCheck").configure { mustRunAfter(extractTask) }
}

// runServer loads the game tests; with -PgameTests it runs them all in CI mode, reports to build/horizonqa and
// exits with their status
tasks.named<JavaExec>("runServer").configure {
  dependsOn(tasks.named("gameTestClasses"))
  classpath(gameTest.output, gameTestMods)
  // -PupdateRecipeIds: the native recipe id snapshot is rewritten from what the mod registers, instead of checked
  if (project.hasProperty("updateRecipeIds")) {
    jvmArgs(
      "-Dminefantasy2tests.updateRecipeIds=" +
        file("src/gameTest/resources/minefantasy2tests/native_recipe_ids.txt").absolutePath,
    )
  }
  if (project.hasProperty("gameTests")) {
    // A world of their own, new for every run: Horizon-QA's void world only applies to a new save, the cells must
    // not dig into the development world, and nothing a previous run left behind may carry over
    val world = layout.projectDirectory.dir("run/horizonqa").asFile
    doFirst { world.deleteRecursively() }
    args("--world", "horizonqa")
    jvmArgs(
      "-Dhorizonqa.mode=ci",
      "-Dhorizonqa.tests=minefantasy2",
      "-Dhorizonqa.reportDir=" + layout.buildDirectory.dir("horizonqa").get().asFile.absolutePath,
    )
  }
}
