plugins {
    id("java")
}

group = "one.jasyncfio"

repositories {
    mavenCentral()
}

tasks.jar {
    manifest.attributes["Main-Class"] = "one.jasyncfio.benchmark.Benchmark"
    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree) // OR .map { zipTree(it) }
    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

dependencies {
    implementation(rootProject)
    implementation("info.picocli:picocli:4.6.3")
}
