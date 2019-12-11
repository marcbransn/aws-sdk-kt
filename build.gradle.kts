
plugins {
    id("org.jetbrains.kotlin.multiplatform") version "1.3.61"
}

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://dl.bintray.com/kotlin/kotlin-dev") }
}

kotlin {
    macosX64("macos") {
        compilations.getByName("main") {
            val openssl by cinterops.creating
        }
        binaries {
            executable {
                entryPoint = "dev.mb.aws.cli.main"
                runTask?.args("")
            }
        }
    }
    sourceSets {
        val macosMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-cli:0.2.0-dev-6")

                val ktorVersion = "1.2.6"
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-curl:$ktorVersion")

                val klockVersion = "1.8.5"
                implementation("com.soywiz.korlibs.klock:klock:$klockVersion")
            }
        }
        val macosTest by getting
    }
}
