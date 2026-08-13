plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
    signing
}

group = "com.pushctl"
version = System.getenv("RELEASE_VERSION") ?: "0.2.0"

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

android {
    namespace = "com.pushctl.sdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(platform("com.google.firebase:firebase-bom:34.16.0"))
    api("com.google.firebase:firebase-messaging")
    implementation("androidx.core:core-ktx:1.16.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.15.1")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
            artifact(javadocJar)
            artifactId = "pushctl-android"
            pom {
                name.set("Pushctl Android SDK")
                description.set("Push notifications and delivery analytics for Android")
                url.set("https://github.com/Pushctl/Pushctl-Android-SDK")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("pushctl")
                        name.set("Pushctl")
                        url.set("https://github.com/Pushctl")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/Pushctl/Pushctl-Android-SDK.git")
                    developerConnection.set("scm:git:ssh://github.com/Pushctl/Pushctl-Android-SDK.git")
                    url.set("https://github.com/Pushctl/Pushctl-Android-SDK")
                }
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Pushctl/Pushctl-Android-SDK")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.key").orNull
            }
        }
        maven {
            name = "MavenCentralStaging"
            url = uri(layout.buildDirectory.dir("maven-central-staging"))
        }
    }
}

signing {
    val signingKey = System.getenv("SIGNING_KEY")
    val signingPassword = System.getenv("SIGNING_PASSWORD")

    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["release"])
    }
}
