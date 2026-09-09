plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.metro)
    alias(libs.plugins.sqldelight)
}

group = "com.bitdesal.taskit"
version = "1.0.0"
application {
    mainClass = "com.bitdesal.taskit.ApplicationKt"
}

sqldelight {
    databases {
        register("TaskItDatabase") {
            packageName.set("com.bitdesal.taskit.db")
        }
    }
}

dependencies {
    api(project(":core"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.sqldelight.sqliteDriver)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}