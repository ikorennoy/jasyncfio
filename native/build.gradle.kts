plugins {
    id("java")
    id("dev.nokee.jni-library")
    id("dev.nokee.c-language")
}

group = "one.jasyncfio"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${deps.junit_jupiter}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${deps.junit_jupiter}")
    testImplementation("org.openjdk.jmh:jmh-core:${deps.jmh_core}")
}

library {
    targetMachines.set(listOf(
        machines.linux.x86_64,
        machines.windows.x86_64,
    ))
}

library.variants.configureEach {
    sharedLibrary.compileTasks.configureEach {
        compilerArgs.add("-D_GNU_SOURCE")
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
