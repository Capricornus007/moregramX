cmake_minimum_required(VERSION 3.22.1 FATAL_ERROR)
project(tgx VERSION 1.0.0 LANGUAGES C CXX)
message(STATUS "Building ${PROJECT_NAME} ndk: r${ANDROID_NDK_MAJOR} (${ANDROID_NDK_REVISION}) platform: ${ANDROID_PLATFORM} (${ANDROID_MIN_SDK_VERSION}) abi: ${ANDROID_ABI} type: ${CMAKE_BUILD_TYPE} flavor: ${TGX_FLAVOR} (${PROJECT_SOURCE_DIR})")

if(NOT ANDROID_NDK_MAJOR OR NOT ANDROID_NDK_REVISION)
  message(FATAL_ERROR "Unknown NDK: r${ANDROID_NDK_MAJOR} (${ANDROID_NDK_REVISION})")
endif()

if (NOT TGX_FLAVOR MATCHES "^(legacy|latest)$")
  message(FATAL_ERROR "Unknown flavor: ${TGX_FLAVOR}")
endif()

if (NOT DEFINED ANDROID_MIN_SDK_VERSION)
  message(FATAL_ERROR "ANDROID_MIN_SDK_VERSION is not set")
endif()

# == Include ==

include("${CMAKE_HOME_DIRECTORY}/cmake/ReadVariables.cmake")
include("${CMAKE_HOME_DIRECTORY}/cmake/Prefix.cmake")
include("${CMAKE_HOME_DIRECTORY}/cmake/Join.cmake")

# == Dirs ==

set(THIRDPARTY_DIR "${PROJECT_SOURCE_DIR}/third_party")
set(TDLIB_DIR "${TGX_ROOT_DIR}/tdlib")
set(UTILS_DIR "${THIRDPARTY_DIR}/jni-utils")

if(${TGX_FLAVOR} STREQUAL "legacy")
  set(ENABLE_TGVOIP no)
else()
  set(ENABLE_TGVOIP yes)
endif()

# Using webp only if building for 32-bit platform
#if (${ANDROID_ABI} STREQUAL "armeabi-v7a" OR ${ANDROID_ABI} STREQUAL "x86")
#  set(USE_WEBP yes)
#else()
#  set(USE_WEBP no)
#endif()
set(USE_WEBP yes)

set(VPX_DIR "${LIBVPX_DIR}/${ANDROID_ABI}")
set(VPX_LIB_PATH "${VPX_DIR}/lib/libvpx.a")

# == Constants ==
# $IS_ARM_FAMILY – armeabi-v7a or arm64-v8a

if (${ANDROID_ABI} STREQUAL "armeabi-v7a" OR ${ANDROID_ABI} STREQUAL "arm64-v8a")
  set(IS_ARM_FAMILY yes)
else()
  set(IS_ARM_FAMILY no)
endif()

# == CMake global settings ==

set(CMAKE_SKIP_RPATH ON)
set(CMAKE_BUILD_WITH_INSTALL_RPATH ON)

if (${IS_ARM_FAMILY})
  enable_language(ASM)
else()
  enable_language(ASM_NASM)
endif()
set(ORIGINAL_CMAKE_ASM_FLAGS "${CMAKE_ASM_FLAGS}")

add_compile_options(
  "$<$<CONFIG:Release,RelWithDebInfo,MinSizeRel>:-ffunction-sections>"
  "$<$<CONFIG:Release,RelWithDebInfo,MinSizeRel>:-fdata-sections>"
  "$<$<CONFIG:Release,RelWithDebInfo,MinSizeRel>:-fvisibility=hidden>"
  "$<$<CONFIG:Release,RelWithDebInfo,MinSizeRel>:-fvisibility-inlines-hidden>"
  "$<$<CONFIG:Release,RelWithDebInfo,MinSizeRel>:-flto=full>"
  "$<$<CONFIG:Release,RelWithDebInfo,MinSizeRel>:-fno-unwind-tables>"
  "$<$<CONFIG:Release,RelWithDebInfo,MinSizeRel>:-fno-asynchronous-unwind-tables>"
)

if (ANDROID_MIN_SDK_VERSION GREATER_EQUAL 23)
  add_link_options(
    "-Wl,--pack-dyn-relocs=android"
  )
endif()

# == Libraries ==

# tdjni

# tdlib ships per-NDK OpenSSL prebuilds and has moved the layout twice:
#   openssl/<ndk>/android-<api>/<abi>/   (current: one build per minSdk)
#   openssl/<ndk>/<abi>/                (previous: one build per NDK)
#   openssl/<abi>/                      (before the per-NDK split)
# Try the toolchain actually in use (ANDROID_NDK_REVISION / ANDROID_PLATFORM come
# from it), then any layer carrying this API+ABI — a legacy-sdk x 64-bit combo
# builds with the legacy NDK while only the primary layer ships arm64 prebuilds.
set(TGX_OPENSSL_DIR "${TDLIB_DIR}/openssl/${ANDROID_NDK_REVISION}/${ANDROID_PLATFORM}/${ANDROID_ABI}")
if(NOT EXISTS "${TGX_OPENSSL_DIR}")
  set(TGX_OPENSSL_DIR "${TDLIB_DIR}/openssl/${ANDROID_NDK_REVISION}/${ANDROID_ABI}")
endif()
if(NOT EXISTS "${TGX_OPENSSL_DIR}")
  set(TGX_OPENSSL_DIR "${TDLIB_DIR}/openssl/${ANDROID_ABI}")
endif()
if(NOT EXISTS "${TGX_OPENSSL_DIR}")
  file(GLOB _tgx_ossl_layers
    "${TDLIB_DIR}/openssl/*/${ANDROID_PLATFORM}/${ANDROID_ABI}"
    "${TDLIB_DIR}/openssl/*/${ANDROID_ABI}")
  if(_tgx_ossl_layers)
    list(GET _tgx_ossl_layers 0 TGX_OPENSSL_DIR)
  endif()
  unset(_tgx_ossl_layers)
endif()
message(STATUS "tgx: OpenSSL prebuilds from ${TGX_OPENSSL_DIR}")

add_library(crypto SHARED IMPORTED)
set_target_properties(crypto PROPERTIES IMPORTED_LOCATION
  ${TGX_OPENSSL_DIR}/lib/libcryptox.so
)
add_library(ssl SHARED IMPORTED)
set_target_properties(ssl PROPERTIES IMPORTED_LOCATION
  ${TGX_OPENSSL_DIR}/lib/libsslx.so
)
if(${CMAKE_VERSION} VERSION_GREATER_EQUAL "3.11.0")
  target_include_directories(crypto INTERFACE
    ${TGX_OPENSSL_DIR}/include
  )
  target_include_directories(ssl INTERFACE
    ${TGX_OPENSSL_DIR}/include
  )
endif()

# tdlib's libtdjni.so uses the same layering as the OpenSSL prebuilds.
set(TGX_TDJNI_DIR "${TDLIB_DIR}/src/main/libs/${ANDROID_NDK_REVISION}/${ANDROID_PLATFORM}/${ANDROID_ABI}")
if(NOT EXISTS "${TGX_TDJNI_DIR}/libtdjni.so")
  set(TGX_TDJNI_DIR "${TDLIB_DIR}/src/main/libs/${ANDROID_NDK_REVISION}/${ANDROID_ABI}")
endif()
if(NOT EXISTS "${TGX_TDJNI_DIR}/libtdjni.so")
  set(TGX_TDJNI_DIR "${TDLIB_DIR}/src/main/libs/${ANDROID_ABI}")
endif()
if(NOT EXISTS "${TGX_TDJNI_DIR}/libtdjni.so")
  file(GLOB _tgx_tdjni_layers
    "${TDLIB_DIR}/src/main/libs/*/${ANDROID_PLATFORM}/${ANDROID_ABI}/libtdjni.so"
    "${TDLIB_DIR}/src/main/libs/*/${ANDROID_ABI}/libtdjni.so")
  if(_tgx_tdjni_layers)
    list(GET _tgx_tdjni_layers 0 _tgx_tdjni_found)
    get_filename_component(TGX_TDJNI_DIR "${_tgx_tdjni_found}" DIRECTORY)
    unset(_tgx_tdjni_layers)
    unset(_tgx_tdjni_found)
  endif()
endif()
message(STATUS "tgx: libtdjni from ${TGX_TDJNI_DIR}")

add_library(tdjni SHARED IMPORTED)
set_target_properties(tdjni PROPERTIES IMPORTED_LOCATION
  ${TGX_TDJNI_DIR}/libtdjni.so
)
target_link_libraries(tdjni INTERFACE crypto ssl)

# opus
include("${PROJECT_SOURCE_DIR}/BuildOpus.cmake")

# ogg
include("${PROJECT_SOURCE_DIR}/BuildOgg.cmake")

# flac
include("${PROJECT_SOURCE_DIR}/BuildFlac.cmake")

# jni-utils
add_subdirectory(
  "${UTILS_DIR}"
)

# yuv
include("${PROJECT_SOURCE_DIR}/BuildYuv.cmake")

# vpx
if(EXISTS "${VPX_LIB_PATH}" AND EXISTS "${VPX_DIR}/include")
  add_library(vpx STATIC IMPORTED)
  set_target_properties(vpx PROPERTIES IMPORTED_LOCATION "${VPX_LIB_PATH}")
  target_include_directories(vpx INTERFACE "${VPX_DIR}/include")
else()
  message(WARNING "./gradlew buildLibvpx was not run yet")
  add_library(vpx INTERFACE)
endif()

# opusfile
include("${PROJECT_SOURCE_DIR}/BuildOpusfile.cmake")

# rlottie
include("${PROJECT_SOURCE_DIR}/BuildRlottie.cmake")

# lz4
include("${PROJECT_SOURCE_DIR}/BuildLz4.cmake")

# webp
if (${USE_WEBP})
  include("${PROJECT_SOURCE_DIR}/BuildWebp.cmake")
endif()

# ffmpeg
foreach(FFMPEG_LIB IN LISTS FFMPEG_LIBS)
  set(FFMPEG_LIB_PATH "${FFMPEG_DIR}/${ANDROID_ABI}/lib/lib${FFMPEG_LIB}.a")
  set(FFMPEG_INCLUDE_DIR "${FFMPEG_DIR}/${ANDROID_ABI}/include")
  if(EXISTS "${FFMPEG_LIB_PATH}" AND EXISTS "${FFMPEG_INCLUDE_DIR}")
    add_library(${FFMPEG_LIB} STATIC IMPORTED)
    set_target_properties(${FFMPEG_LIB} PROPERTIES IMPORTED_LOCATION "${FFMPEG_LIB_PATH}")
    target_include_directories(${FFMPEG_LIB} INTERFACE
      "${FFMPEG_INCLUDE_DIR}"
    )
  else()
    message(WARNING "./gradlew buildFFmpeg was not run yet, missing ${FFMPEG_LIB}")
    add_library(${FFMPEG_LIB} INTERFACE)
  endif()
endforeach()

# whisper.cpp local voice-message transcription
include("${PROJECT_SOURCE_DIR}/BuildWhisper.cmake")

# tgcalls
if (${ENABLE_TGVOIP})
  add_subdirectory(
    "${PROJECT_SOURCE_DIR}/tgvoip"
  )
endif()

# == Target ==

set(NATIVE_LIB "tgxjni")
set(NATIVE_SOURCES
  log.cpp

  "${THIRDPARTY_DIR}/telegram_intro/IntroRenderer.c"

  "${THIRDPARTY_DIR}/emoji_suggestions/emoji_suggestions_data.cpp"
  "${THIRDPARTY_DIR}/emoji_suggestions/emoji_suggestions.cpp"

  jni.c
  voice.c
  emoji.cpp
  utils.cpp
  image.c
  gif.cpp
  views.c
  bridge.cpp
  whisper_jni.cpp
)
if(EXISTS "${ANDROIDX_MEDIA_DIR}")
  list(APPEND NATIVE_SOURCES
    "${ANDROIDX_MEDIA_DIR}/opus_jni.cc"
    "${ANDROIDX_MEDIA_DIR}/flac_jni.cc"
    "${ANDROIDX_MEDIA_DIR}/flac_parser.cc"
    "${ANDROIDX_MEDIA_DIR}/ffmpeg_jni.cc"
    "${ANDROIDX_MEDIA_DIR}/vpx_jni.cc"
  )
else()
  message(WARNING "./gradlew patchJetpackMedia was not run yet")
endif()

add_library(${NATIVE_LIB} SHARED ${NATIVE_SOURCES})
target_include_directories(${NATIVE_LIB} PRIVATE
  "${THIRDPARTY_DIR}/telegram_intro"
  "${ANDROIDX_MEDIA_DIR}"

  "${THIRDPARTY_DIR}"
  .

  "${TGX_ROOT_DIR}/thirdparty/whispercpp/include"
  "${TGX_ROOT_DIR}/thirdparty/whispercpp/ggml/include"
  "${THIRDPARTY_DIR}/opusfile/include"
)

target_compile_definitions(${NATIVE_LIB} PUBLIC
  SOCKLEN_T=socklen_t
  LOCALE_NOT_USED

  DISABLE_IMPORTGL
  BSD=1
  AVOID_TABLES
  ANDROID_TILE_BASED_DECODE
  ANDROID_ARMV6_IDCT
  __STDC_CONSTANT_MACROS

  TDLIB_TDAPI_CLASS_PATH="org/drinkless/tdlib/TdApi"
)
target_compile_options(${NATIVE_LIB} PRIVATE
  -Wall -Werror -Wno-deprecated-declarations
  -Wno-macro-redefined -Wno-unused-variable
  -fno-math-errno -fno-strict-aliasing -funroll-loops
  -fno-rtti
)
# TODO: remove -Wno-macro-redefined -Wno-unused-variable
# only "${THIRDPARTY_DIR}/androidx-media/ffmpeg_jni.cc" needs them.

# == Linking dependencies ==

target_link_libraries(${NATIVE_LIB}
  crypto
  ssl
  tdjni
  jni-utils
  FLAC
  yuv
  vpx
  ogg
  opusfile
  opus
  rlottie
  lz4
  whisper
)
if (${USE_WEBP})
  target_link_libraries(${NATIVE_LIB} webpdecoder_static)
else()
  target_compile_definitions(${NATIVE_LIB} PRIVATE NO_WEBP)
endif()
foreach(ffmpeg_lib ${FFMPEG_LIBS})
  target_link_libraries(${NATIVE_LIB} "${ffmpeg_lib}")
endforeach()

target_link_libraries(${NATIVE_LIB}
  jnigraphics
  log
  GLESv2
  EGL
  android
  cpufeatures
)

target_link_options(${NATIVE_LIB} PRIVATE
  "$<$<CONFIG:Release,RelWithDebInfo,MinSizeRel>:-Wl,--gc-sections,--icf=safe>"
  "$<$<CONFIG:Release,RelWithDebInfo,MinSizeRel>:-Wl,--exclude-libs,ALL>"
  "$<$<CONFIG:Release,RelWithDebInfo,MinSizeRel>:-flto=full>"
)

if (${ANDROID_ABI} STREQUAL "arm64-v8a" OR ${ANDROID_ABI} STREQUAL "x86_64")
  # Enable 16 KB ELF alignment.
  target_link_options(${NATIVE_LIB} PRIVATE
    "-Wl,-z,max-page-size=16384"
  )
endif()

include(AndroidNdkModules)
android_ndk_import_module_cpufeatures()
