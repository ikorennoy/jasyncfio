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

tasks.withType(CCompile::class.java) {
    compilerArgs.add("-O3")
//    compilerArgs.add("-ggdb")
}

tasks.register("renameNativeLib", Copy::class.java) {
    println("copy")
    dependsOn(tasks.getByPath(":sharedLibrary"))
    var arch = System.getProperty("os.arch", "unknown")
    arch = if (arch.equals("x86_64", true) || arch.equals("amd64", true)) {
        "x86_64"
    } else if (arch.equals("aarch64", true)) {
        "aarch64"
    } else {
        throw StopActionException("$arch is not supported")
    }

    from("build/libs/main")
    include("libjasyncfio.so")
    destinationDir = file("build/libs/main")
    rename("libjasyncfio.so", "libjasyncfio-$arch.so")
    file("libjasyncfio.so").delete()
}


dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}
