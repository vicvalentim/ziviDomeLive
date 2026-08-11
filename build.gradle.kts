// This Gradle script is designed to help you build and release your Processing library.
// The section marked "USER BUILD CONFIGURATIONS" is intended for customization.
// The rest of the script is responsible for the build process and should typically not be modified.


import java.util.Properties
import java.time.Instant
import java.nio.file.Files
import java.util.zip.ZipFile
import groovy.json.JsonOutput
import org.gradle.internal.os.OperatingSystem
import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult

plugins {
    id("java")
}

// Sets the Java version to use for compiling your library.
// Processing4 was compiled with Java version 17, so it's recommended to compile your library with version 17.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}


//==========================
// USER BUILD CONFIGURATIONS
//==========================

// the short name of your library. This string will name relevant files and folders.
// Such as:
// <libName>.jar will be the name of your build jar
// <libName>.zip will be the name of your release file
val libName = "ziviDomeLive"

// The group ID of your library, which uniquely identifies your project.
// It's often written in reverse domain name notation.
// For example, if your website is "myDomain.com", your group ID would be "com.myDomain".
// Replace "com.myDomain" with your own domain or organization name.
group = "com.victorvalentim.zividomelive"

// The version of your library. It usually follows semantic versioning (semver),
// which uses three numbers separated by dots: "MAJOR.MINOR.PATCH" (e.g., "1.0.0").
// - MAJOR: Increases when you make incompatible changes.
// - MINOR: Increases when you add new features that are backward-compatible.
// - PATCH: Increases when you make backward-compatible bug fixes.
// You can update these numbers as you release new versions of your library.
version = "1.5.0"

tasks.register("verifyReleaseTag") {
    group = "verification"
    description = "Checks that the requested Git tag matches the project version"

    doLast {
        val expectedTag = "v${project.version}"
        val actualTag = providers.gradleProperty("releaseTag").orNull
            ?: throw GradleException("Missing -PreleaseTag=<tag>; expected $expectedTag")
        check(actualTag == expectedTag) {
            "Release tag $actualTag does not match project version $expectedTag"
        }
        logger.lifecycle("Release tag verified: $actualTag")
    }
}

// Centralized dependency versions for easier Maven sync/updates.
val processingCoreVersion = "4.5.6"
val devolayVersion = "2.2.0-vic.1"

// The location of your sketchbook folder. The sketchbook folder holds your installed
// libraries, tools, and modes. It is needed if you:
// 1. wish to copy the library to the Processing sketchbook, which installs the library locally
// 2. have Processing library dependencies
// Depending on your OS, the code below should set the correct location, if you are using a Mac,
// Windows, or Linux machine.
// If you run the Gradle task deployToProcessingSketchbook, and you do not see your library
// in the contributions manager, then one possible cause could be the sketchbook location
// is wrong. You can check the sketchbook location in your Processing application preferences.
var sketchbookLocation = ""
val userHome = System.getProperty("user.home")
val currentOS = OperatingSystem.current()
if (currentOS.isMacOsX) {
    sketchbookLocation = "$userHome/Documents/Processing/"
} else if (currentOS.isWindows) {
    sketchbookLocation = "$userHome/My Documents/Processing/sketchbook"
} else {
    sketchbookLocation = "$userHome/sketchbook"
}
// If you need to set the sketchbook location manually, uncomment out the following
// line and set sketchbookLocation to the correct location
// sketchbookLocation = "$userHome/sketchbook"


// Repositories where dependencies will be fetched from.
// You can add additional repositories here if your dependencies are hosted elsewhere.
repositories {
    mavenCentral()

    // Kept for compatibility with occasional transitive dependencies in Processing ecosystem.
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://jogamp.org/deployment/maven/") }
}

// Add any external dependencies your library requires here.
// The provided example uses Apache Commons Math. Replace or add as needed.
dependencies {
    // resolve Processing core
    compileOnly(group = "org.processing", name = "core", version = processingCoreVersion)

    // insert your external dependencies
    implementation(group = "io.github.vicvalentim", name = "devolay", version = devolayVersion)
    //implementation(group = "org.apache.commons", name = "commons-math3", version = "3.6.1")
    // The provided example uses commons-math3. Replace or add as needed.

    // Bibliotecas locais no sketchbook (ControlP5, Syphon, SpoutProcessing)
    compileOnly(fileTree("src/main/libs/controlP5.jar"))
    compileOnly(fileTree("src/main/libs/spout.jar"))
    compileOnly(fileTree("src/main/libs/Syphon.jar"))

    // To add a dependency on a Processing library that is installed locally,
    // uncomment the line below, and replace <library folder> with the location of that library
    // compileOnly(fileTree("$sketchbookLocation/libraries/<library folder>/library"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // Processing core is needed at test runtime for PApplet static math helpers
    // (sin, cos, sqrt, constrain) used by Quaternion and related classes.
    testImplementation(group = "org.processing", name = "core", version = processingCoreVersion)
    // Local sketchbook libraries are needed at test runtime to load classes
    // that reference ControlP5/Syphon/Spout types (zividomelive, managers).
    testRuntimeOnly(fileTree("src/main/libs/controlP5.jar"))
    testRuntimeOnly(fileTree("src/main/libs/spout.jar"))
    testRuntimeOnly(fileTree("src/main/libs/Syphon.jar"))
}

tasks.test {
    useJUnitPlatform()
}

val qualificationResultsDirectory = layout.buildDirectory.dir("test-results/qualification")
val qualificationReportDirectory = layout.buildDirectory.dir("reports/qualification")

tasks.register<Test>("qualificationTests") {
    group = "verification"
    description = "Runs the complete automated qualification suite and writes auditable reports"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    dependsOn(tasks.named("testClasses"))
    mustRunAfter("clean")
    useJUnitPlatform()

    // Qualification runs are explicit evidence, so they must not reuse an old
    // up-to-date result and should collect every failure in one pass.
    outputs.upToDateWhen { false }
    failFast = false
    maxParallelForks = 1
    systemProperty("java.awt.headless", "true")

    reports {
        junitXml.required.set(true)
        junitXml.outputLocation.set(qualificationResultsDirectory)
        html.required.set(true)
        html.outputLocation.set(qualificationReportDirectory.map { it.dir("tests") })
    }

    testLogging {
        events("failed", "skipped")
        showStandardStreams = false
    }

    addTestListener(object : TestListener {
        override fun beforeSuite(suite: TestDescriptor) = Unit
        override fun beforeTest(testDescriptor: TestDescriptor) = Unit
        override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) = Unit

        override fun afterSuite(suite: TestDescriptor, result: TestResult) {
            if (suite.parent != null) {
                return
            }

            val reportDirectory = qualificationReportDirectory.get().asFile
            reportDirectory.mkdirs()

            val status = if (result.failedTestCount == 0L) "passed" else "failed"
            val summary = linkedMapOf(
                "schemaVersion" to 1,
                "project" to project.name,
                "version" to project.version.toString(),
                "status" to status,
                "startedAt" to Instant.ofEpochMilli(result.startTime).toString(),
                "completedAt" to Instant.ofEpochMilli(result.endTime).toString(),
                "durationMs" to result.endTime - result.startTime,
                "tests" to linkedMapOf(
                    "total" to result.testCount,
                    "passed" to result.successfulTestCount,
                    "failed" to result.failedTestCount,
                    "skipped" to result.skippedTestCount
                ),
                "environment" to linkedMapOf(
                    "javaVersion" to System.getProperty("java.version"),
                    "osName" to System.getProperty("os.name"),
                    "osVersion" to System.getProperty("os.version"),
                    "architecture" to System.getProperty("os.arch"),
                    "ciRevision" to (System.getenv("GITHUB_SHA") ?: "local-worktree")
                )
            )

            reportDirectory.resolve("summary.json").writeText(
                JsonOutput.prettyPrint(JsonOutput.toJson(summary)) + System.lineSeparator()
            )
            reportDirectory.resolve("summary.md").writeText(
                """# ziviDomeLive Automated Qualification

- Status: ${status.uppercase()}
- Version: ${project.version}
- Tests: ${result.testCount} total, ${result.successfulTestCount} passed, ${result.failedTestCount} failed, ${result.skippedTestCount} skipped
- Duration: ${result.endTime - result.startTime} ms
- Completed: ${Instant.ofEpochMilli(result.endTime)}
- Environment: ${System.getProperty("os.name")} ${System.getProperty("os.version")} (${System.getProperty("os.arch")}), Java ${System.getProperty("java.version")}
- Revision: ${System.getenv("GITHUB_SHA") ?: "local-worktree"}

Detailed HTML results: `tests/index.html`
JUnit XML results: `../../test-results/qualification/`
"""
            )

            logger.lifecycle(
                "Qualification tests: {} total, {} passed, {} failed, {} skipped. Report: {}",
                result.testCount,
                result.successfulTestCount,
                result.failedTestCount,
                result.skippedTestCount,
                reportDirectory.resolve("summary.md")
            )
        }
    })
}

// Downloads pinned legacy libraries that are not available on Maven.
val requiredLocalLibraries = listOf(
    file("src/main/libs/controlP5.jar"),
    file("src/main/libs/spout.jar"),
    file("src/main/libs/Syphon.jar")
)

tasks.register<Exec>("downloadDependencies") {
    group = "processing"
    description = "Downloads checksum-verified ControlP5, Syphon, and Spout dependencies"
    onlyIf {
        requiredLocalLibraries.any { !it.isFile }
    }
    commandLine("bash", "$rootDir/download_dependencies.sh")
}

tasks.named("compileJava") {
    dependsOn("downloadDependencies")
}

//==============================
// END USER BUILD CONFIGURATIONS
//==============================


// =============================
// INTERNAL BUILD CONFIGURATIONS
// Do not edit the following sections unless you know what you're doing.
// =============================

// Settings for how the JAR file (your library) will be built.
// You want to name your jar with the library short name, aka libName.
tasks.jar {
    archiveBaseName.set(libName)
    archiveClassifier.set("")
    archiveVersion.set("")

    from("shaders") {
        into("data/shaders")
    }
}


// ===========================
// Tasks for releasing library
// ===========================

val releaseRoot = "$rootDir/release"
val releaseName = libName
val releaseDirectory = "$releaseRoot/$releaseName"

// read in user-defined properties in release.properties file
// to be saved in library.properties file, a required file in the release
// using task writeLibraryProperties
val libraryProperties = Properties().apply {
    load(rootProject.file("release.properties").inputStream())
}

tasks.register<WriteProperties>("writeLibraryProperties") {
    group = "processing"
    destinationFile = project.file("library.properties")

    property("name", libraryProperties.getProperty("name"))
    property("version", libraryProperties.getProperty("version"))
    property("prettyVersion", project.version)
    property("authors", libraryProperties.getProperty("authors"))
    property("url", libraryProperties.getProperty("url"))
    property("categories", libraryProperties.getProperty("categories"))
    property("sentence", libraryProperties.getProperty("sentence"))
    property("paragraph", libraryProperties.getProperty("paragraph"))
    property("minRevision", libraryProperties.getProperty("minRevision"))
    property("maxRevision", libraryProperties.getProperty("maxRevision"))
    property("tested.platform", libraryProperties.getProperty("tested.platform"))
    property("tested.processingVersion", libraryProperties.getProperty("tested.processingVersion"))
    property("library.copyright", libraryProperties.getProperty("library.copyright"))
    property("library.dependencies", libraryProperties.getProperty("library.dependencies"))
    property("library.keywords", libraryProperties.getProperty("library.keywords"))
}

// define the order of running, to ensure clean is run first
tasks.build.get().mustRunAfter("clean")
tasks.assemble.get().mustRunAfter("clean")
tasks.javadoc.get().mustRunAfter("assemble")

tasks.register("buildReleaseArtifacts") {
    group = "processing"
    dependsOn("clean", "assemble", "javadoc", "writeLibraryProperties")
    finalizedBy("packageRelease", "duplicateZipToPdex")

    doFirst {
        println("Releasing library $libName")
        println(org.gradle.internal.jvm.Jvm.current())

        println("Cleaning release...")
        project.delete(files(releaseRoot))
    }

    doLast {
        println("Creating package...")

        println("Copy library...")
        copy {
            from(layout.buildDirectory.file("libs/${libName}.jar"))
            into("$releaseDirectory/library")
        }

        println("Copy dependencies...")
        copy {
            from(configurations.runtimeClasspath)
            into("$releaseDirectory/library")
        }

        println("Copy assets...")
        copy {
            from("$rootDir")
            include("data/**", "native/**")

            into("$releaseDirectory/library")
            exclude("*.DS_Store")
        }

        println("Copy javadoc...")
        copy {
            from(layout.buildDirectory.dir("docs/javadoc"))
            into("$releaseDirectory/reference")
        }

        println("Copy additional artifacts...")
        copy {
            from(rootDir)
            include(
                "README.md",
                "LICENSE",
                "CHANGELOG.md",
                "CITATION.cff",
                ".zenodo.json",
                "THIRD_PARTY.md",
                "licenses/**",
                "readme/**",
                "library.properties",
                "examples/**",
                "src/**"
            )

            into(releaseDirectory)
            exclude("**/*.DS_Store", "**/networks/**", "src/test/**", "src/main/libs/**")
        }

        println("Copy repository library.txt...")
        copy {
            from(rootDir)
            include("library.properties")
            into(releaseRoot)
            rename("library.properties", "$libName.txt")
        }
    }
}

tasks.register<Zip>("packageRelease") {
    dependsOn("buildReleaseArtifacts")
    doFirst {
        println("Create zip file...")
    }
    archiveFileName.set("${libName}.zip")
    from(releaseDirectory)
    into(releaseName)
    destinationDirectory.set(file(releaseRoot))
    exclude("**/*.DS_Store")
}

tasks.register<Copy>("duplicateZipToPdex") {
    doFirst {
        println("Duplicate zip file to pdex extension...")
    }
    from(releaseRoot) {
        include("$libName.zip")
        rename("$libName.zip", "$libName.pdex")
    }
    into(releaseRoot)
}
tasks["duplicateZipToPdex"].mustRunAfter("packageRelease")

val verifyProcessingPackage = tasks.register("verifyProcessingPackage") {
    group = "verification"
    description = "Verifies release archives and prevents development tests from shipping"

    doLast {
        val requiredPackageFiles = listOf(
            "README.md",
            "LICENSE",
            "CHANGELOG.md",
            "CITATION.cff",
            ".zenodo.json",
            "THIRD_PARTY.md",
            "licenses/Apache-2.0.txt",
            "library.properties",
            "library/${libName}.jar",
            "library/devolay-${devolayVersion}.jar"
        )
        val missingPackageFiles = requiredPackageFiles.filterNot {
            file("$releaseDirectory/$it").isFile
        }
        check(missingPackageFiles.isEmpty()) {
            "Processing release is missing required files: ${missingPackageFiles.joinToString()}"
        }

        val forbiddenReleaseFiles = fileTree(releaseDirectory) {
            include("src/test/**", "src/main/libs/**", "**/.DS_Store")
        }.files
        check(forbiddenReleaseFiles.isEmpty()) {
            "Processing release contains development-only files: ${forbiddenReleaseFiles.joinToString()}"
        }

        val zipFile = file("$releaseRoot/$libName.zip")
        val pdexFile = file("$releaseRoot/$libName.pdex")
        val metadataFile = file("$releaseDirectory/library.properties")
        val contributionMetadataFile = file("$releaseRoot/$libName.txt")
        check(zipFile.isFile) { "Missing release archive: $zipFile" }
        check(pdexFile.isFile) { "Missing Processing package: $pdexFile" }
        check(contributionMetadataFile.isFile) {
            "Missing Processing contribution metadata: $contributionMetadataFile"
        }
        check(Files.mismatch(metadataFile.toPath(), contributionMetadataFile.toPath()) == -1L) {
            "$libName.txt must match the packaged library.properties"
        }

        val packagedMetadata = Properties().apply {
            metadataFile.inputStream().use { load(it) }
        }
        check(packagedMetadata.getProperty("prettyVersion") == project.version.toString()) {
            "Packaged prettyVersion does not match project version ${project.version}"
        }
        check(packagedMetadata.getProperty("version") == libraryProperties.getProperty("version")) {
            "Packaged Processing release counter does not match release.properties"
        }

        listOf(zipFile, pdexFile).forEach { archive ->
            val archiveEntries = mutableSetOf<String>()
            ZipFile(archive).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    archiveEntries.add(entries.nextElement().name)
                }
            }

            val requiredEntries = requiredPackageFiles.map { "$releaseName/$it" }
            val missingEntries = requiredEntries.filterNot(archiveEntries::contains)
            check(missingEntries.isEmpty()) {
                "${archive.name} is missing required entries: ${missingEntries.joinToString()}"
            }

            val forbiddenEntries = archiveEntries.filter { name ->
                name == "$releaseName/src/test"
                    || name.startsWith("$releaseName/src/test/")
                    || name == "$releaseName/src/main/libs"
                    || name.startsWith("$releaseName/src/main/libs/")
                    || name.endsWith("/.DS_Store")
                    || (name.endsWith(".jar") && !name.startsWith("$releaseName/library/"))
            }
            check(forbiddenEntries.isEmpty()) {
                "${archive.name} contains development-only entries: ${forbiddenEntries.joinToString()}"
            }
        }

        check(Files.mismatch(zipFile.toPath(), pdexFile.toPath()) == -1L) {
            "$libName.zip and $libName.pdex must be byte-identical"
        }

        logger.lifecycle(
            "Processing package verified: metadata and legal files present; "
                + "development-only files absent; ZIP and PDEX are byte-identical."
        )
    }
}

tasks["duplicateZipToPdex"].finalizedBy(verifyProcessingPackage)

tasks.register("deployToProcessingSketchbook") {
    group = "processing"
    description = "Installs the release package in the local Processing sketchbook"
    dependsOn("buildReleaseArtifacts")

    val installDirectory = "$sketchbookLocation/libraries/$libName"

    doLast {
        println("Copy to sketchbook  $sketchbookLocation ...")
        project.delete(file("$installDirectory/src/test"))
        copy {
            from(releaseDirectory)
            include(
                "README.md",
                "LICENSE",
                "CHANGELOG.md",
                "CITATION.cff",
                ".zenodo.json",
                "THIRD_PARTY.md",
                "licenses/**",
                "library.properties",
                "examples/**",
                "library/**",
                "reference/**",
                "src/**"
            )
            exclude("src/test/**", "src/main/libs/**")
            into(installDirectory)
        }
    }
}
