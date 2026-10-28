@file:Suppress("UnstableApiUsage", "AvoidApplyPluginMethod")

apply(from = "properties.gradle.kts")

pluginManagement {
  repositories {
    google()
    mavenCentral()
    maven(url = "https://jitpack.io")
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven(url = "https://jitpack.io")
    if (extra["huawei"] == true) {
      maven(url = "https://developer.huawei.com/repo/")
    }
  }
}

// tgx/main aa687c67c：baseline-profile 移進 :tests:baseline-profile 並一律 include，
// 上方的 `if (generateBaselineProfile) include(":baseline-profile")` 連同那個頂層目錄
// 一起消失（我方早先已把頂層 baseline-profile 刪掉，這條條件其實是空頭支票）。
// 產生 baseline profile 在本 fork 仍由 -PgenerateBaselineProfile 控制，但那是
// app/build.gradle.kts 決定 consumer 變體的事，這裡不需要 include 之外的旗標。
rootProject.name = "moeGramX"
includeBuild("vkryl/annotation")
include(
  ":tdlib",
  ":tgcalls",

  ":vkryl:td",
  ":vkryl:android",
  ":vkryl:leveldb",
  ":vkryl:core",

  ":extension:bridge",
  ":extension:${extra["extension"]}",

  ":app",

  ":tests:baseline-profile",
  ":tests:core",
  ":tests:ui",
  ":tests:benchmark",
  ":tests:microbenchmark"
)
