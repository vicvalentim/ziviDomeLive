import java.util.zip.ZipFile
import org.gradle.external.javadoc.StandardJavadocDocletOptions

plugins {
    `java-library`
    `maven-publish`
}

group = "com.victorvalentim.zividomelive"
version = "0.1.0-SNAPSHOT"

base {
    archivesName.set("zividomelive-core")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    // Public APIs carry focused narrative documentation; do not require repetitive tag-only
    // prose while retaining every non-missing doclint check.
    (options as StandardJavadocDocletOptions)
        .addStringOption("Xdoclint:all,-missing", "-quiet")
}

publishing {
    publications {
        create<MavenPublication>("core") {
            from(components["java"])
            artifactId = "zividomelive-core"
            pom {
                name.set("ziviDomeLive Core")
                description.set("Platform-independent Java 17 semantics and state for ziviDomeLive hosts.")
                url.set("https://github.com/vicvalentim/ziviDomeLive")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("vicvalentim")
                        name.set("Victor Valentim")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/vicvalentim/ziviDomeLive.git")
                    developerConnection.set("scm:git:ssh://git@github.com/vicvalentim/ziviDomeLive.git")
                    url.set("https://github.com/vicvalentim/ziviDomeLive")
                }
            }
        }
    }
    repositories {
        maven {
            name = "coreTest"
            url = layout.buildDirectory.dir("maven-test-repository").get().asFile.toURI()
        }
    }
}

val forbiddenSourceReferences = listOf(
    "processing.",
    "com.jogamp.",
    "org.lwjgl.",
    "controlP5.",
    "codeanticode.syphon.",
    "devolay.",
    "spout.",
    "java.awt.",
    "javax.swing.",
    "com.victorvalentim.zividomelive.ziviDomeLive",
    "com.victorvalentim.zividomelive.Scene",
    "com.victorvalentim.zividomelive.render."
)

val forbiddenBytecodeReferences = forbiddenSourceReferences.map { it.replace('.', '/') }

val verifyCoreSourceBoundary = tasks.register("verifyCoreSourceBoundary") {
    group = "verification"
    description = "Rejects host, graphics backend, facade, and Processing references in Core source."
    inputs.files(sourceSets.main.get().allSource)

    doLast {
        val violations = sourceSets.main.get().allSource.files.flatMap { source ->
            val text = source.readText()
            forbiddenSourceReferences.filter(text::contains).map { reference ->
                "${source.relativeTo(projectDir)} -> $reference"
            }
        }
        check(violations.isEmpty()) {
            "Core source boundary violations:\n${violations.joinToString("\n")}"
        }
    }
}

val verifyCoreJarBoundary = tasks.register("verifyCoreJarBoundary") {
    group = "verification"
    description = "Rejects forbidden symbolic references in the compiled Core JAR."
    dependsOn(tasks.jar)
    inputs.file(tasks.jar.flatMap { it.archiveFile })

    doLast {
        val jarFile = tasks.jar.get().archiveFile.get().asFile
        val violations = mutableListOf<String>()
        ZipFile(jarFile).use { zip ->
            zip.entries().asSequence()
                .filter { !it.isDirectory && it.name.endsWith(".class") }
                .forEach { entry ->
                    val symbols = zip.getInputStream(entry).readBytes().toString(Charsets.ISO_8859_1)
                    forbiddenBytecodeReferences.filter(symbols::contains).forEach { reference ->
                        violations += "${entry.name} -> $reference"
                    }
                }
        }
        check(violations.isEmpty()) {
            "Core JAR boundary violations:\n${violations.joinToString("\n")}"
        }
    }
}

tasks.register("verifyCoreBoundary") {
    group = "verification"
    description = "Runs source and bytecode independence checks for ziviDomeLive Core."
    dependsOn(verifyCoreSourceBoundary, verifyCoreJarBoundary)
}

tasks.check {
    dependsOn("verifyCoreBoundary")
}
