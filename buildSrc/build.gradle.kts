@file:Suppress("AvoidApplyPluginMethod")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
  // Kotlin 2.5.0-Beta1 explicitly, and it must be this line rather than `libs.kotlin` below:
  // `kotlin-dsl` applies org.jetbrains.kotlin.jvm itself, at whatever Kotlin Gradle is embedding
  // (Gradle 9.9.0-M2 embeds 2.4.20 — docs "Embedded Kotlin version" table, and confirmed against
  // the distribution zip), and 2.4.x's `JvmTarget` enum stops at JVM_26. Writing JVM_27 without
  // this line does not compile: proven by running Gradle's own 2.4.10 compiler on
  // kotlin-gradle-plugin-api-2.4.20.jar -> "unresolved reference 'JVM_27'", same source against
  // kotlin-gradle-plugin-api-2.5.0-Beta1.jar -> compiles. Keep this in sync with the `kotlin`
  // version in gradle/libs.versions.toml.
  // That also means buildSrc classes are emitted with 2.5.0 metadata while app/build.gradle.kts is
  // compiled by the embedded 2.4.20 script compiler (it imports tgx.gradle.*) — tolerated: the
  // 2.4.10 compiler above read 2.5.0-Beta1 metadata without a compatibility complaint.
  id("org.jetbrains.kotlin.jvm") version "2.5.0-Beta1"
  `kotlin-dsl`
}

// Java 27 = class file major version 71. AGP 9.5.0-alpha08's pom declares org.ow2.asm 9.9 (plus
// asm-analysis / asm-commons / asm-util), whose Opcodes stop at V26 = 70 — verified with
// `javap -constants` on asm-9.9.1.jar; asm-9.10.1 is the release that adds V27 = 71, and it is
// also exactly what Gradle 9.8/9.9 themselves bundle, so nothing exotic is being introduced.
// The force belongs HERE because this build loads AGP off buildSrc's classpath: lines 75-76
// (`implementation(libs.android.gradle.plugin)` / `libs.kotlin.gradle.plugin`) are what put AGP
// and the Kotlin plugin on the classpath of every project, and `app/build.gradle.kts:16` applies
// `id("com.android.application")` with no version at all (it resolves from this classpath, not
// from a plugin marker). A project-level or root-level force would never reach it, and
// xposedsmscode's root `buildscript {}` shape does not apply to this repo's layout.
configurations.all {
  resolutionStrategy {
    force("org.ow2.asm:asm:9.10.1")
    force("org.ow2.asm:asm-analysis:9.10.1")
    force("org.ow2.asm:asm-commons:9.10.1")
    force("org.ow2.asm:asm-tree:9.10.1")
    force("org.ow2.asm:asm-util:9.10.1")
  }
}

java {
  toolchain {
    languageVersion = JavaLanguageVersion.of(27)
  }
}

kotlin {
  compilerOptions {
    allWarningsAsErrors = true
    jvmTarget = JvmTarget.JVM_27
    // `kotlin-dsl` 把 languageVersion 釘在 2.2，而 KGP 2.5.0-Beta1 已視 2.2 為棄用，buildSrc
    // 又開 allWarningsAsErrors，於是 :buildSrc:compileKotlin 紅在
    // 「Language version 2.2 is deprecated ... Update the version to 2.3」。抬到 2.3 正面修，
    // apiVersion 一起抬（兩個不一致會觸發另一條診斷），而不是把 allWarningsAsErrors 關掉。
    // 常數存在性查過：kotlin-gradle-plugin-api 的 KotlinVersion 是列舉，2.4.0 那份就有
    // KOTLIN_2_3（javap 實查），不是照文件猜的。
    languageVersion = KotlinVersion.KOTLIN_2_3
    apiVersion = KotlinVersion.KOTLIN_2_3
    // 專案層設了还不夠：CI #177/#163 顯示 `:buildSrc:compileKotlin` 仍拿 2.2，代表 kotlin-dsl
    // 是在 KotlinCompile **任務**上蓋值（不是專案層的 compilerOptions）。下面另有一段任務級的
    // configureEach。suppressVersionWarnings 是 KGP 專門給「語言版本棄用」這一類警告的開關，
    // 只壓這一款；其他警告照樣被 allWarningsAsErrors 擋下。
    // suppressVersionWarnings 不是 KGP 的 DSL 屬性（CI #178/#164 實測
    // 「Unresolved reference」），這是一款編譯器旗標，只能走 freeCompilerArgs。
    freeCompilerArgs.add("-Xsuppress-version-warnings")
  }
  jvmToolchain {
    languageVersion = JavaLanguageVersion.of(27)
  }
}

// 任務級再設一次：configureEach 的註冊順序在 kotlin-dsl 之後，所以這裡的值會覆蓋它釘的 2.2。
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions {
    languageVersion = KotlinVersion.KOTLIN_2_3
    apiVersion = KotlinVersion.KOTLIN_2_3
    freeCompilerArgs.add("-Xsuppress-version-warnings")
  }
}

gradlePlugin {
  plugins {
    register("tgx-config") {
      id = "tgx-config"
      implementationClass = "tgx.gradle.plugin.AppConfigurationPlugin"
    }
    register("tgx-module") {
      id = "tgx-module"
      implementationClass = "tgx.gradle.plugin.ModulePlugin"
    }
  }
}

dependencies {
  // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
  implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

  compileOnly(gradleApi())
  implementation(libs.android.gradle.plugin)
  implementation(libs.kotlin.gradle.plugin)
  implementation(libs.okhttp.latest)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.jgit)
  implementation(libs.jgit.lfs)
}

apply(from = "${rootDir.parentFile}/properties.gradle.kts")
if (extra["huawei"] == true) {
  dependencies {
    implementation(libs.huawei.agconnect)
  }
}