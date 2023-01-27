import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    `maven-publish`
    signing
    id("me.champeau.jmh") version(deps.jmh_plugin)
    id("com.github.johnrengelman.shadow") version(deps.shadow_plugin)
    id("io.github.gradle-nexus.publish-plugin") version(deps.nexus_publish)
}

val jvmTargetVersion = "8"

group = "one.jasyncfio"


tasks.filterIsInstance<JavaCompile>().forEach { compileJava ->
    compileJava.targetCompatibility = jvmTargetVersion
    compileJava.sourceCompatibility = jvmTargetVersion
}


repositories {
    mavenCentral()
}


// compile shared lib

val arch: String
    get() {
        val archString = System.getProperty("os.arch")
        return if ("x86_64".equals(archString, true) || "amd64".equals(archString, true)) {
            "amd64"
        } else if ("aarch64".equals(archString, true)) {
            "arm64"
        } else {
            throw IllegalArgumentException("Architecture $archString is not supported")
        }
    }

val jdkPath: File
    get() {
        val javaHome = System.getenv("JAVA_HOME") ?: "/usr/lib/jvm/java-8-openjdk-$arch"
        return File(javaHome)
    }

println("JAVA_HOME: $jdkPath")
println("ARCH: $arch")

val cWorkDir = project.file("src/main/c")
val objectsOutputDir = project.file("build/generated")
val sharedLib = project.file("build/libjasyncfio.so").absolutePath

// all targets
val syscallTarget = File(objectsOutputDir, "syscall.o")
val javaIoUringNativesTarget = File(objectsOutputDir, "java_io_uring_natives.o")
val ioUringConstantsTarget = File(objectsOutputDir, "io_uring_constants.o")
val fileIoConstantsTarget = File(objectsOutputDir, "file_io_constants.o")


task("fileIoConstants", Exec::class) {
    val fileIoConstantsSource = project.file("src/main/c/file_io_constants.c")
    workingDir = cWorkDir
    commandLine = getCompileObjectArgs(fileIoConstantsSource, fileIoConstantsTarget)
}

task("ioUringConstants", Exec::class) {
    val ioUringConstantsSrc = project.file("src/main/c/io_uring_constants.c")
    workingDir = cWorkDir
    commandLine = getCompileObjectArgs(ioUringConstantsSrc, ioUringConstantsTarget)
}

task("javaIoUringNatives", Exec::class) {
    val javaIoUringNativesSource = project.file("src/main/c/java_io_uring_natives.c")
    workingDir = cWorkDir
    commandLine = getCompileObjectArgs(javaIoUringNativesSource, javaIoUringNativesTarget)
}

task("syscall", Exec::class) {
    val syscallSource = project.file("src/main/c/syscall.c")
    workingDir = cWorkDir
    commandLine = getCompileObjectArgs(syscallSource, syscallTarget)
}

task("sharedLib", Exec::class) {
    dependsOn(
        tasks.getByName("fileIoConstants"),
        tasks.getByName("ioUringConstants"),
        tasks.getByName("javaIoUringNatives"),
        tasks.getByName("syscall")
    )
    commandLine = listOf(
        "gcc",
        "-shared",
        "-o",
        sharedLib,
        syscallTarget.absolutePath,
        javaIoUringNativesTarget.absolutePath,
        ioUringConstantsTarget.absolutePath,
        fileIoConstantsTarget.absolutePath
    )
}

fun getCompileObjectArgs(sourceFile: File, outputFile: File): List<String> {
    return listOf(
        "gcc",
        "-c",
        "-D_GNU_SOURCE",
        "-fpic",
        "-Wall",
        "-Wcast-qual",
        "-Wshadow",
        "-Wformat=2",
        "-Wundef",
        "-Werror=float-equal",
        "-Werror=strict-prototypes",
        "-o",
        outputFile.absolutePath,
        "-I",
        "${jdkPath.absolutePath}/include",
        "-I",
        "${jdkPath.absolutePath}/include/linux",
        sourceFile.absolutePath
    )
}

tasks.withType(ShadowJar::class) {
    dependsOn.add(tasks.getByName("sharedLib"))
    from(sharedLib)
    archiveClassifier.set("linux-$arch")
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Bundle-Description" to "jasyncfio provides an API for working with files through the Linux io_uring interface"
            ),
        )
    }
    mergeServiceFiles()
    minimize()
}

tasks.withType(Test::class) {
    dependsOn.add(tasks.getByName("shadowJar"))
    useJUnitPlatform()
}

tasks.withType<Sign>().configureEach {
    onlyIf { !version.toString().endsWith("SNAPSHOT") }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name.set("jasyncfio")
                description.set("jasyncfio provides an API for working with files through the Linux io_uring interface")
                url.set("https://github.com/ikorennoy/jasyncfio")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("ikorennoy")
                        name.set("Ilya Korennoy")
                        email.set("korennoy.ilya@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/ikorennoy/jasyncfio")
                    developerConnection.set("scm:git:ssh://git@github.com/ikorennoy/jasyncfio.git")
                    url.set("https://github.com/ikorennoy/jasyncfio")
                }
            }
            groupId = "one.jasyncfio"
            artifactId = "jasyncfio"
            version = version
            java.withJavadocJar()
            java.withSourcesJar()
            artifact(tasks.getByName("shadowJar"))
            artifact(tasks.getByName("javadocJar"))
            artifact(tasks.getByName("sourcesJar"))
        }
    }
    signing {
        useGpgCmd()
        sign(publishing.publications["maven"])
    }
}


nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(System.getenv("S_USERNAME"))
            password.set(System.getenv("S_PASSWORD"))
        }
    }
}


dependencies {
    implementation("org.jctools:jctools-core:${deps.jctools}")
    implementation("cn.danielw:fast-object-pool:${deps.object_pool}")
    implementation("com.conversantmedia:disruptor:${deps.object_pool_disruptor}")
    implementation("com.tdunning:t-digest:${deps.t_digest}")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${deps.junit_jupiter}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${deps.junit_jupiter}")
    testImplementation("org.openjdk.jmh:jmh-core:${deps.jmh_core}")
}
