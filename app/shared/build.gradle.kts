import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.metro)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    jvm()
    
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    android {
       namespace = "com.bitdesal.taskit.app.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
       withDeviceTestBuilder {
           sourceSetTreeName = "test"
       }.configure {
           instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
       }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            api(project(":core"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
            implementation(libs.ktor.client.js)
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.cio.wasm.js)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

val taskitEnv = providers.gradleProperty("taskit.env").orElse("debug")
val releaseApiUrl = providers.gradleProperty("taskit.api.url.release")
    .orElse("https://api.taskit.bitdesal.com")
val debugApiUrl = providers.gradleProperty("taskit.api.url.debug")
    .orElse("http://127.0.0.1:8080")
val debugAndroidApiUrl = providers.gradleProperty("taskit.api.url.debug.android")
    .orElse("http://10.0.2.2:8080")

fun apiBaseUrl(platform: String): Provider<String> = taskitEnv.flatMap { env ->
    val production = env.equals("release", ignoreCase = true) ||
        env.equals("prod", ignoreCase = true)
    when {
        production -> releaseApiUrl
        platform == "android" -> debugAndroidApiUrl
        else -> debugApiUrl
    }
}

fun registerAppConfigActual(sourceSetName: String, platform: String): TaskProvider<Task> {
    val outputDir = layout.buildDirectory.dir("generated/appconfig/$sourceSetName")
    val baseUrl = apiBaseUrl(platform)
    val task = tasks.register("generateAppConfig${sourceSetName.replaceFirstChar { it.uppercase() }}") {
        group = "build"
        description = "Generates AppConfig.BASE_URL for $sourceSetName"
        inputs.property("baseUrl", baseUrl)
        outputs.dir(outputDir)
        val output = outputDir
        val url = baseUrl
        doLast {
            val packageDir = output.get().asFile.resolve("com/bitdesal/taskit/config")
            packageDir.mkdirs()
            val literal = url.get()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\$", "\\\$")
            packageDir.resolve("AppConfig.kt").writeText(
                """
                package com.bitdesal.taskit.config

                internal actual object AppConfig {
                    actual val BASE_URL: String = "$literal"
                }

                """.trimIndent() + "\n",
            )
        }
    }
    kotlin.sourceSets.named(sourceSetName) {
        kotlin.srcDir(task)
    }
    return task
}

registerAppConfigActual("androidMain", "android")
registerAppConfigActual("jvmMain", "jvm")
registerAppConfigActual("jsMain", "js")
registerAppConfigActual("wasmJsMain", "wasmJs")
