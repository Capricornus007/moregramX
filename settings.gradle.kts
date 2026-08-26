val generateBaselineProfile = providers.gradleProperty("generateBaselineProfile").map {
  it.toBoolean()
}.orElse(false)

rootProject.name = "moeGramX"
if (generateBaselineProfile.get()) {
