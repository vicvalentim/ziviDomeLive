import java.util.zip.ZipFile
import java.io.ByteArrayOutputStream
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

val externalConsumerDirectory = layout.buildDirectory.dir("external-consumer")
val externalConsumerOutput = ByteArrayOutputStream()

tasks.register<Exec>("externalConsumerSmoke") {
    group = "verification"
    description = "Publishes Core locally, then compiles and runs an independent Maven consumer."
    dependsOn("publishCorePublicationToCoreTestRepository")
    outputs.upToDateWhen { false }
    isIgnoreExitValue = true

    doFirst {
        val consumerDirectory = externalConsumerDirectory.get().asFile
        delete(consumerDirectory)
        consumerDirectory.resolve("src/main/java/smoke").mkdirs()
        consumerDirectory.resolve("settings.gradle.kts").writeText(
            "rootProject.name = \"zividomelive-core-external-consumer\"\n"
        )
        val repositoryUri = layout.buildDirectory.dir("maven-test-repository")
            .get().asFile.toURI().toASCIIString()
        consumerDirectory.resolve("build.gradle.kts").writeText(
            """
            plugins {
                application
            }

            repositories {
                maven { url = uri("$repositoryUri") }
            }

            dependencies {
                implementation("com.victorvalentim.zividomelive:zividomelive-core:0.1.0-SNAPSHOT")
            }

            java {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(17))
                }
            }

            application {
                mainClass.set("smoke.CoreConsumerSmoke")
            }
            """.trimIndent() + "\n"
        )
        consumerDirectory.resolve("src/main/java/smoke/CoreConsumerSmoke.java").writeText(
            """
            package smoke;

            import com.victorvalentim.zividomelive.core.action.ActionMap;
            import com.victorvalentim.zividomelive.core.camera.OrbitCamera;
            import com.victorvalentim.zividomelive.core.math.Quaternion;
            import com.victorvalentim.zividomelive.core.projection.SphericalOrientation;
            import com.victorvalentim.zividomelive.core.time.FrameClock;

            import java.util.concurrent.atomic.AtomicInteger;
            import java.util.concurrent.atomic.AtomicLong;

            public final class CoreConsumerSmoke {
                private CoreConsumerSmoke() {
                }

                public static void main(String[] args) {
                    AtomicLong nanos = new AtomicLong(1_000_000_000L);
                    FrameClock clock = new FrameClock(nanos::get);
                    clock.tick();
                    nanos.addAndGet(16_000_000L);
                    clock.tick();

                    Quaternion turn = Quaternion.fromAxisAngle(0.0f, 1.0f, 0.0f, 0.25f);
                    SphericalOrientation spherical = new SphericalOrientation();
                    spherical.setYaw(0.25f);
                    OrbitCamera camera = new OrbitCamera(-300.0f);
                    camera.setDistanceLimits(-1000.0f, 1000.0f);
                    camera.setOrientationImmediate(turn);

                    AtomicInteger actionsRun = new AtomicInteger();
                    try (ActionMap actions = new ActionMap()) {
                        actions.register("verify", actionsRun::incrementAndGet);
                        if (!actions.trigger("verify")) {
                            throw new IllegalStateException("Core action was not triggered.");
                        }
                    }

                    if (clock.getFrameIndex() != 2L
                            || actionsRun.get() != 1
                            || camera.getDistance() != -300.0f
                            || spherical.getQuaternion().w() != turn.w()) {
                        throw new IllegalStateException("Core consumer invariant failed.");
                    }
                    System.out.println("CORE_EXTERNAL_CONSUMER_OK frame="
                            + clock.getFrameIndex() + " distance=" + camera.getDistance());
                }
            }
            """.trimIndent() + "\n"
        )

        externalConsumerOutput.reset()
        workingDir(consumerDirectory)
        val wrapper = rootProject.file(
            if (System.getProperty("os.name").lowercase().contains("windows")) {
                "gradlew.bat"
            } else {
                "gradlew"
            }
        )
        commandLine(wrapper.absolutePath, "--no-daemon", "--offline", "--console=plain", "clean", "run")
        standardOutput = externalConsumerOutput
        errorOutput = externalConsumerOutput
    }

    doLast {
        val output = externalConsumerOutput.toString(Charsets.UTF_8)
        logger.lifecycle(output.trim())
        check(executionResult.get().exitValue == 0) {
            "Independent Core consumer build failed."
        }
        check(output.contains("CORE_EXTERNAL_CONSUMER_OK")) {
            "Independent Core consumer did not print CORE_EXTERNAL_CONSUMER_OK."
        }
    }
}
