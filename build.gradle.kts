import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    implementation(libs.sqlite.jdbc)
    implementation(libs.ucanaccess)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.swing)
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "com.dbstudio.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "DataBaseStudio"
            packageVersion = "1.0.0"
            description = "DataBase Studio - SQLite & Access Manager"
            vendor = "DataBaseStudio"

            modules(
                "java.base",
                "java.desktop",
                "java.sql",
                "java.sql.rowset",
                "java.naming",
                "java.management",
                "java.instrument",
                "java.logging",
                "java.prefs",
                "java.xml",
                "jdk.crypto.ec",
                "jdk.unsupported"
            )

            windows {
                menuGroup = "DataBase Studio"
                upgradeUuid = "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
        }
    }
}

