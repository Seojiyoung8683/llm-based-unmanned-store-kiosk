pluginManagement {
    repositories { gradlePluginPortal(); google(); mavenCentral() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "kioskfinal"

include(":admin-app", ":kiosk-app")
project(":admin-app").projectDir = file("admin")
project(":kiosk-app").projectDir = file("kiosk")
