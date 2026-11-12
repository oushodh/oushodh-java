plugins {
    `java-library`
    id("com.diffplug.spotless") version "8.11.0"
    id("com.vanniktech.maven.publish") version "0.37.0"
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.jspecify:jspecify:1.0.1")
    implementation("tools.jackson.core:jackson-databind:3.3.0")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    // Built with the newest LTS JDK, run on Java 17 and later, Android included.
    options.release = 17
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.jar {
    manifest {
        attributes("Automatic-Module-Name" to "dev.oushodh")
    }
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("oushodh.version", version.toString())
}

spotless {
    java {
        googleJavaFormat("2.0.0")
    }
    kotlinGradle {
        ktlint()
    }
}

mavenPublishing {
    publishToMavenCentral()

    // Local and JitPack builds have no key, so only a release signs.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }

    coordinates("dev.oushodh", "oushodh", version.toString())

    pom {
        name = "Oushodh"
        description = "The Java SDK for the Oushodh medicine data API."
        url = "https://github.com/oushodh/oushodh-java"
        inceptionYear = "2026"

        licenses {
            license {
                name = "MIT"
                url = "https://opensource.org/license/mit"
            }
        }

        developers {
            developer {
                id = "oushodh"
                name = "Oushodh"
                email = "support@oushodh.dev"
            }
        }

        scm {
            url = "https://github.com/oushodh/oushodh-java"
            connection = "scm:git:https://github.com/oushodh/oushodh-java.git"
            developerConnection = "scm:git:ssh://git@github.com/oushodh/oushodh-java.git"
        }
    }
}
