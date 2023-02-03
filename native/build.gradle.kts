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


tasks.getByName<Test>("test") {
    useJUnitPlatform()
}