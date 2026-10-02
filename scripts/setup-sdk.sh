#!/bin/bash
set -e
# shellcheck source=set-env.sh
source "$(dirname "$0")"/set-env.sh "$1"

# Fetching SDK
echo "Checking Android SDK: ${ANDROID_SDK_ROOT}..." && (test -d "$ANDROID_SDK_ROOT" || mkdir -p "$ANDROID_SDK_ROOT")

# Fetching cmdline-tools
CMDLINE_VERSION=$(read-property.sh version.properties version.cmdline_tools)

pushd "$ANDROID_SDK_ROOT"
if [ ! -d "$ANDROID_SDK_ROOT/cmdline-tools/latest" ]; then
  test -f cmdline-tools.zip || (echo "Downloading cmdline-tools..." && wget -O cmdline-tools.zip "https://dl.google.com/android/repository/commandlinetools-${PLATFORM}-${CMDLINE_VERSION}_latest.zip")
  echo "Installing cmdline-tools..."
  unzip cmdline-tools.zip -d cmdline-tools
  mv -f cmdline-tools/cmdline-tools cmdline-tools/latest
fi
popd

# Downloading packages
BUILD_TOOLS_VERSION=$(read-property.sh version.properties version.build_tools)
COMPILE_SDK_VERSION=$(read-property.sh version.properties version.sdk_compile)
SDK_PACKAGE=$(read-property.sh version.properties version.sdk_package)
CMAKE_VERSION=$(read-property.sh version.properties version.cmake)
ANDROID_NDK_VERSION_PRIMARY=$(read-property.sh version.properties version.ndk_primary)

yes | "$ANDROID_SDK_ROOT"/cmdline-tools/latest/bin/sdkmanager --licenses
yes | "$ANDROID_SDK_ROOT"/cmdline-tools/latest/bin/sdkmanager --update
yes | "$ANDROID_SDK_ROOT"/cmdline-tools/latest/bin/sdkmanager --install \
  "platforms;$SDK_PACKAGE" \
  "build-tools;$BUILD_TOOLS_VERSION" \
  "ndk;$ANDROID_NDK_VERSION_PRIMARY" \
  "cmake;$CMAKE_VERSION"

test -d "$ANDROID_SDK_ROOT" || { echo "ANDROID_SDK_ROOT ($ANDROID_SDK_ROOT) not found!" >&2; exit 1; }
test -d "$ANDROID_SDK_ROOT/ndk/$ANDROID_NDK_VERSION_PRIMARY" || { echo "ANDROID_NDK ($ANDROID_NDK_VERSION_PRIMARY) not found!" >&2; exit 1; }
# 我方已移除 legacy NDK (r23) 安裝步驟與 flavor，故上游重新撿回的 ndk_legacy 檢查不予採用，
# 否則 setup-sdk.sh 會在「沒裝 r23」的機器上直接 exit 1（規則 14：我方較佳且確認重複 → 去重丟棄）。

echo "SDK setup is now complete!"
echo "build-tools: ${BUILD_TOOLS_VERSION}, ndk: ${ANDROID_NDK_VERSION_PRIMARY}"