plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
    signing
}

val versionName: String by project
val publishGroupId: String by project
val publishArtifactId: String by project

android {
    namespace = "com.erkan.zvt"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// ── Publishing Configuration ──

val sourceJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(android.sourceSets["main"].java.srcDirs)
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = publishGroupId
                artifactId = publishArtifactId
                version = versionName

                artifact(sourceJar)
                artifact(javadocJar)

                pom {
                    name.set("Kotlin ZVT Client Library")
                    description.set("Android Kotlin library implementing the ZVT protocol for ECR-to-payment-terminal communication over TCP/IP")
                    url.set("https://github.com/kaplanerkan/kotlin-zvt-library")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }

                    developers {
                        developer {
                            id.set("kaplanerkan")
                            name.set("Erkan Kaplan")
                        }
                    }

                    scm {
                        url.set("https://github.com/kaplanerkan/kotlin-zvt-library")
                        connection.set("scm:git:github.com/kaplanerkan/kotlin-zvt-library.git")
                        developerConnection.set("scm:git:ssh://github.com/kaplanerkan/kotlin-zvt-library.git")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "sonatype"
                val releasesUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                val snapshotsUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
                url = if (versionName.endsWith("-SNAPSHOT")) snapshotsUrl else releasesUrl

                credentials {
                    username = findProperty("ossrhUsername")?.toString() ?: System.getenv("OSSRH_USERNAME") ?: ""
                    password = findProperty("ossrhPassword")?.toString() ?: System.getenv("OSSRH_PASSWORD") ?: ""
                }
            }
        }
    }

    signing {
        val signingKeyId = findProperty("signing.keyId")?.toString() ?: System.getenv("SIGNING_KEY_ID")
        val signingKey = findProperty("signing.key")?.toString() ?: System.getenv("SIGNING_KEY")
        val signingPassword = findProperty("signing.password")?.toString() ?: System.getenv("SIGNING_PASSWORD")

        if (signingKeyId != null && signingKey != null && signingPassword != null) {
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        }

        sign(publishing.publications["release"])
    }
}
