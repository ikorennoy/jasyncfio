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


tasks.withType(ShadowJar::class) {
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
    implementation(project(":native"))
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${deps.junit_jupiter}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${deps.junit_jupiter}")
    testImplementation("org.openjdk.jmh:jmh-core:${deps.jmh_core}")
}
