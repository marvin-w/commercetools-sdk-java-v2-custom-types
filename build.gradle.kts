import org.jetbrains.dokka.gradle.DokkaTask
import java.time.Instant
import java.time.Duration

extra["isReleaseVersion"] = !version.toString().endsWith("SNAPSHOT")

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka") apply false
    signing
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin")
}

allprojects {
    buildscript {
        repositories {
            mavenCentral()
            mavenLocal()
        }
    }

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

subprojects {
    val kotlinJvmVersion: String by project

    val currentProject = this

    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "signing")
    apply(plugin = "maven-publish")

    tasks {
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                jvmTarget = kotlinJvmVersion
                freeCompilerArgs = listOf("-Xjsr305=strict")
            }
        }

        jar {
            manifest {
                attributes["Built-By"] = "Akii"
                attributes["Build-Jdk"] = "${System.getProperty("java.version")} (${System.getProperty("java.vendor")} ${System.getProperty("java.vm.version")})"
                attributes["Build-Timestamp"] = Instant.now().toString()
                attributes["Created-By"] = "Gradle ${gradle.gradleVersion}"
                attributes["Implementation-Title"] = currentProject.name
                attributes["Implementation-Version"] = project.version
            }
        }

        java {
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        val jarComponent = currentProject.components.getByName("java")
        val sourcesJar by registering(Jar::class) {
            archiveClassifier.set("sources")
            from(sourceSets.main.get().allSource)
        }

        val dokka = named("dokkaJavadoc", DokkaTask::class)
        val javadocJar by registering(Jar::class) {
            archiveClassifier.set("javadoc")
            from("$buildDir/dokka/javadoc")
            dependsOn(dokka)
        }

        publishing {
            publications {
                withType<MavenPublication> {
                    pom {
                        name.set("${currentProject.group}:${currentProject.name}")
                        url.set("https://github.com/Akii/commercetools-sdk-java-v2-custom-types")
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                            }
                        }
                        developers {
                            developer {
                                name.set("Akii")
                                email.set("oss@akii.de")
                            }
                        }
                        scm {
                            connection.set("scm:git:git://github.com:Akii/commercetools-sdk-java-v2-custom-types.git")
                            developerConnection.set("scm:git:git://github.com:Akii/commercetools-sdk-java-v2-custom-types.git")
                            url.set("https://github.com/Akii/commercetools-sdk-java-v2-custom-types")
                        }

                        val mavenPom = this
                        afterEvaluate {
                            mavenPom.description.set(currentProject.description)
                        }
                    }
                }
                create<MavenPublication>("mavenJava") {
                    from(jarComponent)

                    if (rootProject.extra["isReleaseVersion"] as Boolean) {
                        artifact(sourcesJar.get())
                        artifact(javadocJar.get())
                    }
                }
            }
        }
    }
    signing {
        setRequired {
            (rootProject.extra["isReleaseVersion"] as Boolean) && (gradle.taskGraph.hasTask("publish") || gradle.taskGraph.hasTask("publishPlugins"))
        }
        val signingKey: String? = System.getenv("PGP_SIGN_KEY") ?: project.findProperty("pgp.sign.key") as String?
        val signingPassword: String? = System.getenv("PGP_SIGN_PASSPHRASE") ?: project.findProperty("pgp.sign.passphrase") as String?
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }

    dependencies {
        implementation(kotlin("stdlib"))
        testImplementation(kotlin("test"))
    }

    tasks.test {
        useJUnitPlatform()
    }
}

tasks {
    jar {
        enabled = false
    }
    nexusPublishing {
        repositories {
            sonatype {
                nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
                snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
                username.set(System.getenv("SONATYPE_PUBLISH_USERNAME") ?: project.findProperty("sonatype.publish.username") as String?)
                password.set(System.getenv("SONATYPE_PUBLISH_PASSWORD") ?: project.findProperty("sonatype.publish.password") as String?)
            }
        }

        transitionCheckOptions {
            maxRetries.set(60)
            delayBetween.set(Duration.ofMillis(5000))
        }
    }
}
