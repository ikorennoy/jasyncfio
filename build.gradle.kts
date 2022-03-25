plugins {
    java
    `maven-publish`
    id("me.champeau.jmh") version "0.6.1"
}

val jvmTargetVersion = "8"

group = "one.jasyncfio"
version = "0.0.1"

tasks.filterIsInstance<JavaCompile>().forEach { compileJava ->
    compileJava.targetCompatibility = jvmTargetVersion
    compileJava.sourceCompatibility = jvmTargetVersion
}


val cWorkDir = "src/main/c"
val jdkPath = System.getenv("JAVA_HOME") ?: "/home/ikorennoy/.jdks/temurin-1.8.0_322"
val jdkHome = File(jdkPath).normalize()
val sharedLib = "build/libjasyncfio.so"


repositories {
    mavenCentral()
}

task("compileLib", Exec::class) {
    workingDir = File(cWorkDir).absoluteFile
    println("JDK: $jdkHome")
    println("Working dir: $workingDir")
    println("Shared lib: ${File(sharedLib).absolutePath}")

    commandLine = listOf(
        "gcc",
        "-g",
        "-Ofast",
        "-shared",
        "-fpic",
        "-o",
        File(sharedLib).absolutePath,
        "-I",
        "$jdkHome/include/",
        "-I",
        "$jdkHome/include/linux/",
        "java_io_uring_natives.c"
    )
}

tasks.jar {
    dependsOn.add(tasks.getByName("compileLib"))
    from(sharedLib)
    archiveClassifier.set("linux-x86_64")
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Bundle-Description" to "jasyncfio provides an API for working with files through the Linux io_uring interface"
            ),
        )
    }
}

tasks.withType(Test::class) {
    useJUnitPlatform()
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
            from(components["java"])
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}
