// فایل تنظیمات اصلی Gradle؛ نام پروژه و ماژول‌های قابل ساخت را مشخص می‌کند.
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}

rootProject.name = "App-NameDic"
include(":app")
