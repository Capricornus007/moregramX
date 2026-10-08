plugins.withType<JavaBasePlugin> {
  extensions.configure<JavaPluginExtension> {
    toolchain {
      // Java 27 for every project that ships code (app, tdlib, tgcalls, vkryl:*, extension:*,
      // baseline-profile). Gradle's compatibility matrix documents Java 27 as supported for both
      // toolchains and for running Gradle from 9.8.0 on; this build has no foojay resolver, so
      // the JDK must really exist — both workflows install 27 before calling Gradle.
      languageVersion.set(JavaLanguageVersion.of(27))
    }
  }
}