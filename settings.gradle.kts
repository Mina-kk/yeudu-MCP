pluginManagement {
    buildscript {
        repositories { mavenCentral(); google(); maven { url = uri("https://storage.googleapis.com/r8-releases/raw") } }
        dependencies { classpath("com.android.tools:r8:9.1.29") }
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("$rootDir/third_party/maven") }
        google()
        mavenCentral()
    }
}
rootProject.name = "LegadoSourceStudio"
include(":app")
include(":legado-rhino")
