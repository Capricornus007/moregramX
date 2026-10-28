/*
 * This file is a part of Telegram X
 * Copyright © 2014 (tgx-android@pm.me)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package tgx.gradle

import Abi
import AbiVariant
import Sdk
import SdkVariant
import com.android.build.api.dsl.BaseFlavor
import com.android.build.api.dsl.VariantDimension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.FilterConfiguration
import com.android.build.api.variant.TestAndroidComponentsExtension
import com.android.build.api.variant.VariantOutput
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderConvertible
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.gradle.util.internal.VersionNumber
import tgx.gradle.task.wrapInDoubleQuotes

fun BaseFlavor.buildConfigInt (name: String, value: Int) =
  this.buildConfigField("int", name, value.toString())
fun BaseFlavor.buildConfigLong (name: String, value: Long) =
  this.buildConfigField("long", name, value.toString())
fun BaseFlavor.buildConfigBool (name: String, value: Boolean) =
  this.buildConfigField("boolean", name, value.toString())
fun BaseFlavor.buildConfigString (name: String, value: String?) =
  this.buildConfigField("String", name, value?.wrapInDoubleQuotes() ?: "null")
fun VariantDimension.buildConfigInt (name: String, value: Int) =
  this.buildConfigField("int", name, value.toString())
fun VariantDimension.buildConfigLong (name: String, value: Long) =
  this.buildConfigField("long", name, value.toString())
fun VariantDimension.buildConfigString (name: String, value: String?) =
  this.buildConfigField("String", name, value?.wrapInDoubleQuotes() ?: "null")

fun DependencyHandlerScope.legacyImplementation(
  dependency: Any,
  dependencyConfiguration: Action<ExternalModuleDependency>? = null
) =
  this.registerFlavorImpl("legacy", dependency, dependencyConfiguration)

fun DependencyHandlerScope.sinceLollipopImplementation(
  sinceLollipop: Any,
  sinceMarshmallow: Any? = null,
  dependencyConfiguration: Action<ExternalModuleDependency>? = null
) =
  applyApiFlavor(
    null,
    sinceMarshmallow ?: sinceLollipop,
    dependencyConfiguration
  )

fun DependencyHandlerScope.sinceMarshmallowImplementation(
  sinceMarshmallow: Any,
  sinceNougat: Any? = null,
  dependencyConfiguration: Action<ExternalModuleDependency>? = null
) =
  applyApiFlavor(
    null,
    sinceNougat ?: sinceMarshmallow,
    dependencyConfiguration
  )

fun DependencyHandlerScope.preMarshmallowImplementation(
  legacyAndLollipop: Any,
  dependencyConfiguration: Action<ExternalModuleDependency>? = null
) =
  applyApiFlavor(
    legacyAndLollipop,
    null,
    dependencyConfiguration
  )

fun DependencyHandlerScope.sinceNougatImplementation(
  sinceNougat: Any,
  dependencyConfiguration: Action<ExternalModuleDependency>? = null
) =
  applyApiFlavor(
    null,
    sinceNougat,
    dependencyConfiguration
  )

fun findExtraFolders(variant: SdkVariant): Set<String> =
  mutableSetOf<String>().apply {
    if (variant.minSdk >= 21) {
      this += "sinceLollipop"
    }
    if (variant.minSdk < 23) {
      this += "preMarshmallow"
    }
    if (variant.minSdk >= 23 || variant.isLatest) {
      this += "sinceMarshmallow"
    }
    if (variant.supportsSdk(26)) {
      this += "sinceOreo"
    }
    this += "only${variant.flavor.uppercaseFirstChar()}"
  }.toSet()

fun <T> selectApiFlavor(
  variant: SdkVariant,
  legacy: T,
  latest: T
): T =
  when (variant.flavor) {
    "legacy" -> legacy
    "latest" -> latest
    else -> error(variant.flavor)
  }

fun <T> selectAbiFlavor(
  variant: AbiVariant,
  universal: T,
  arm32: T,
  arm64: T,
  x86: T,
  x64: T,
  lab: T
): T =
  when (variant.flavor) {
    "universal" -> universal
    "arm32" -> arm32
    "arm64" -> arm64
    "x86" -> x86
    "x64", "x86_64" -> x64
    "lab" -> lab
    else -> error(variant.flavor)
  }

private fun DependencyHandlerScope.registerFlavorImpl(
  flavor: String,
  dependency: Any?,
  dependencyConfiguration: Action<ExternalModuleDependency>? = null
) {
  if (dependency != null) {
    if (dependencyConfiguration != null) {
      when (dependency) {
        is String ->
          "${flavor}Implementation"(dependency) {
            dependencyConfiguration.execute(this)
          }
        is Provider<*> ->
          "${flavor}Implementation"(dependency) {
            dependencyConfiguration.execute(this)
          }
        is ProviderConvertible<*> ->
          "${flavor}Implementation"(dependency) {
            dependencyConfiguration.execute(this)
          }
        else -> {
          error("Unknown type: $dependency of ${dependency.javaClass}")
        }
      }
    } else {
      "${flavor}Implementation"(dependency)
    }
  }
}

// Terminal implementation for the two-slot (legacy, latest) model. Every SDK variant
// currently registered (only `latest`) resolves through selectApiFlavor to the `latest`
// argument; the `legacy` slot is kept only so the pinned submodules' first argument still
// type-checks. This is private so the public `flavorImplementation` overloads below stay
// unambiguous for three-positional calls.
private fun DependencyHandlerScope.applyApiFlavor(
  legacy: Any?,
  latest: Any?,
  dependencyConfiguration: Action<ExternalModuleDependency>? = null
) {
  Sdk.VARIANTS.values.forEach { sdkVariant ->
    val library = selectApiFlavor(
      sdkVariant,
      legacy,
      latest
    )
    registerFlavorImpl(sdkVariant.flavor, library, dependencyConfiguration)
  }
}

fun DependencyHandlerScope.flavorImplementation(
  legacy: Any?,
  latest: Any?,
  dependencyConfiguration: Action<ExternalModuleDependency>? = null
) = applyApiFlavor(legacy, latest, dependencyConfiguration)

// Compat shim: the pinned vkryl/X-Core and vkryl/X-Android submodules still call the
// three-argument form (legacy, lollipop, latest). Only the `latest` SDK flavor builds now,
// so the middle (former lollipop) slot is ignored and forwarded straight to `latest`.
fun DependencyHandlerScope.flavorImplementation(
  legacy: Any?,
  @Suppress("UNUSED_PARAMETER") lollipop: Any?,
  latest: Any?,
  dependencyConfiguration: Action<ExternalModuleDependency>? = null
) = applyApiFlavor(legacy, latest, dependencyConfiguration)

// Compat shim for tgx/main aa687c67c: app/build.gradle.kts now passes four slots
// (legacy, lollipop, marshmallow, latest) — e.g. androidx.palette and
// play-services-maps. Same reasoning as above: only `latest` is built here, so the two
// middle slots are ignored and the call forwards to the two-slot terminal form.
fun DependencyHandlerScope.flavorImplementation(
  legacy: Any?,
  @Suppress("UNUSED_PARAMETER") lollipop: Any?,
  @Suppress("UNUSED_PARAMETER") marshmallow: Any?,
  latest: Any?,
  dependencyConfiguration: Action<ExternalModuleDependency>? = null
) = applyApiFlavor(legacy, latest, dependencyConfiguration)

fun isVariantEnabled(sdkVariant: SdkVariant, abiVariant: AbiVariant, isDebug: Boolean): Boolean =
  sdkVariant.minSdk >= abiVariant.minSdk &&
  !(abiVariant.flavor == "universal" && sdkVariant.flavor == "legacy")

fun ApplicationAndroidComponentsExtension.disableRudimentaryVariants(
  filter: (SdkVariant, AbiVariant) -> Boolean = { _, _ -> true }
) =
  beforeVariants { variantBuilder ->
    val sdkFlavor = variantBuilder.productFlavors.first { it.first == "SDK" }.second
    val sdkVariant = Sdk.VARIANTS.values.first { it.flavor == sdkFlavor }
    val abiFlavor = variantBuilder.productFlavors.first { it.first == "ABI" }.second
    val abiVariant = Abi.VARIANTS.values.first { it.flavor == abiFlavor }
    val isDebug = variantBuilder.buildType == "debug"
    // Only published builds are limited: debug and baseline profile builds must install on any device
    if (sdkVariant.maxSdk != null && variantBuilder.buildType == "release") {
      variantBuilder.maxSdk = sdkVariant.maxSdk
    }
    variantBuilder.enable = isVariantEnabled(sdkVariant, abiVariant, isDebug) && filter(sdkVariant, abiVariant)
  }

fun TestAndroidComponentsExtension.disableRudimentaryVariants(
  filter: (SdkVariant, AbiVariant) -> Boolean = { _, _ -> true }
) =
  beforeVariants { variantBuilder ->
    val sdkFlavor = variantBuilder.productFlavors.first { it.first == "SDK" }.second
    val sdkVariant = Sdk.VARIANTS.values.first { it.flavor == sdkFlavor }
    val abiFlavor = variantBuilder.productFlavors.first { it.first == "ABI" }.second
    val abiVariant = Abi.VARIANTS.values.first { it.flavor == abiFlavor }
    val isDebug = variantBuilder.buildType == "debug"
    // Only published builds are limited: debug and baseline profile builds must install on any device
    if (sdkVariant.maxSdk != null && variantBuilder.buildType == "release") {
      variantBuilder.maxSdk = sdkVariant.maxSdk
    }
    variantBuilder.enable = isVariantEnabled(sdkVariant, abiVariant, isDebug) && filter(sdkVariant, abiVariant)
  }

fun findHostAbi(): String =
  if (System.getProperty("os.arch") in listOf("aarch64", "arm64")) {
    "arm64"
  } else {
    "x64"
  }

fun findHostTag(): String {
  val os = System.getProperty("os.name")
  return when {
    os.startsWith("mac", ignoreCase = true) ->
      "darwin-x86_64"
    os.startsWith("Linux", ignoreCase = true) ->
      "linux-x86_64"
    os.startsWith("Windows", ignoreCase = true) ->
      "windows-x86_64"
    else ->
      error("Unknown system: $os")
  }
}

fun String.toAbiFilter(): String = when (this) {
  "arm64" -> "arm64-v8a"
  "arm32" -> "armeabi-v7a"
  "x86" -> "x86"
  "x64" -> "x86_64"
  else -> error("Unknown abi variant: $this")
}

fun String.toAbiVariant(): String = when (this) {
  "arm64-v8a" -> "arm64"
  "armeabi-v7a" -> "arm32"
  "x86" -> "x86"
  "x86_64" -> "x64"
  else -> error("Unknown abi filter: $this")
}

fun String.ndkVersionMajor(): Int =
  VersionNumber.parse(this).major

fun String.ndkVersionToMinSdk(): Int =
  this.ndkVersionMajor().let { major ->
    when {
      major >= 27 -> 21
      major >= 24 -> 19
      major >= 23 -> 16
      else -> error("Unsupported NDK version: $this")
    }
  }

// 以下四個上游（無 ABI flavor 版本）的輔助函式被嫁接進來的
// AppConfigurationPlugin／ModulePlugin 匯入，補回以免斷鏈。
fun VariantOutput.findAbiFilters(ifEmpty: List<String>): List<String> =
  this.filters.filter { filter ->
    filter.filterType == FilterConfiguration.FilterType.ABI
  }.map {
    it.identifier
  }.ifEmpty {
    ifEmpty
  }

fun ApplicationVariant.findAbiFilters(ifEmpty: List<String>): List<String> =
  this.outputs.flatMap { output ->
    output.findAbiFilters(ifEmpty)
  }.distinct()

fun Project.needEnableSplitAbi(abiFilters: Provider<List<String>>): Provider<Boolean> =
  providers.gradleProperty("abi")
    .map { it.isEmpty() }
    .orElse(true)
    .zip(abiFilters) { enableSplitAbi, abiFilters ->
      enableSplitAbi && abiFilters.size > 1
    }

fun Project.findAbiFilters(useLegacyNdk: Provider<Boolean>): Provider<List<String>> =
  providers.gradleProperty("abi")
    .map(Abi::toFilters)
    .orElse(Abi.FILTERS)
    .zip(useLegacyNdk) { abiFilters, useLegacyNdk ->
      abiFilters.filter { abi ->
        abi == "armeabi-v7a" || abi == "x86" || !useLegacyNdk
      }.ifEmpty {
        fatal("Legacy NDK supports only 32-bit ABIs, select arm32 or x86 with -Pabi")
      }
    }
    .zip(providers.gradleProperty("android.injected.build.abi").orElse("")) { abiFilters, injectedAbiList ->
      val injectedAbi = injectedAbiList
        .split(",")
        .filter {
          it in Abi.FILTERS
        }
      injectedAbi.firstOrNull {
        it in abiFilters
      }?.let {
        listOf(it)
      } ?: abiFilters
    }

fun Project.gradleFlag(name: String, defaultValue: Boolean = false): Provider<Boolean> =
  providers.gradleProperty(name).map {
    it.toBoolean()
  }.orElse(defaultValue)