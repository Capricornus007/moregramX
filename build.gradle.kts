// Top-level build file where you can add configuration options common to all sub-projects/modules.
//
// Java 27 note: the org.ow2.asm 9.9 -> 9.10.1 force lives in buildSrc/build.gradle.kts, which is
// the only classpath AGP is loaded from here. It deliberately does NOT live in a root
// `buildscript {}` block (that is what XposedSmsCode needs, because AGP arrives through its root
// plugins-DSL classpath): the two markers below were checked on Google's Maven and neither
// declares com.android.tools.build:gradle (google-services 4.5.0 has no AGP dependency;
// androidx.baselineprofile 1.5.0 -> benchmark-baseline-profile-gradle-plugin ->
// benchmark-gradle-plugin, AGP only compileOnly), so a force on this classpath would be a no-op
// that only *looks* like a fix.
plugins {
  id("java-toolchain-convention")
  alias(libs.plugins.google.services) apply false
  alias(libs.plugins.androidx.baselineprofile) apply false
}
