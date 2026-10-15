pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "Chukta"
include(":shared", ":androidApp")
project(":androidApp").projectDir = file("androidApp/app")
