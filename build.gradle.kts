plugins {
    java
    id("dev.nokee.jni-library")
    id("dev.nokee.c-language")
//    id "me.champeau.jmh" version "0.6.5"
}

group = "one.jasyncfio"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.jar {
    manifest {
        attributes(mapOf("Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Bundle-Description" to "jasyncfio provides an API for working with files through the Linux io_uring interface"),
        )
    }
}

tasks.withType(CCompile::class.java) {
//    compilerArgs.add("-ggdb")
    compilerArgs.add("-D_GNU_SOURCE")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}
