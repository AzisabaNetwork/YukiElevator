plugins {
    kotlin("jvm") version "2.4.21"
    id("com.gradleup.shadow") version "9.6.1"
}

group = "net.azisaba"
version = "3.8.2+1.21.11"
description = "シンプルなエレベータープラグイン"

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation(kotlin("stdlib"))

    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

tasks {
    jar {
        archiveClassifier.set("plain")
    }

    shadowJar {
        archiveClassifier.set("")
        // 他プラグインが同梱する Kotlin と衝突しないようにリロケートする
        relocate("kotlin", "net.azisaba.yukielevator.libs.kotlin")
        relocate("org.jetbrains.annotations", "net.azisaba.yukielevator.libs.jetbrains.annotations")
        relocate("org.intellij.lang.annotations", "net.azisaba.yukielevator.libs.intellij.annotations")
        exclude("META-INF/*.kotlin_module", "META-INF/versions/**", "META-INF/maven/**")
        minimize()
    }

    build {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
