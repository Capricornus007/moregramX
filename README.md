# moregramX (MGX)

[![Android CI](https://github.com/Capricornus007/moreGramX/actions/workflows/build.yml/badge.svg)](https://github.com/Capricornus007/moreGramX/actions/workflows/build.yml)
[![Java Compile Check](https://github.com/Capricornus007/moreGramX/actions/workflows/java-check.yml/badge.svg)](https://github.com/Capricornus007/moreGramX/actions/workflows/java-check.yml)
[![License: GPLv3](https://img.shields.io/badge/license-GPLv3-blue.svg)](LICENSE)

moregramX is an independent Telegram client for Android based on
[Telegram X](https://github.com/TGX-Android/Telegram-X) and
[TDLib](https://github.com/tdlib/td). It combines Telegram X with selected
features and fixes from [moeGramX](https://github.com/moeCrafters/moeGramX),
[reX](https://github.com/logopek/reX), and this repository's own work.

The current application version is **1826**, based on Telegram X **1805**.

> This is an unofficial project. It is not affiliated with, maintained by, or
> endorsed by Telegram, Telegram X, moeGramX, or reX.

## Highlights

### reX integration

- Forum-topic layouts, saved forum style, topic icons, per-topic
  notifications, and forum stability fixes.
- Quote or reply to a message in another chat.
- Profile channel, profile audio/music, and profile-note support.
- Configurable reX settings and input-panel buttons.
- Additional topic, reaction, and attachment-button controls.
- On-device voice-message transcription using
  [whisper.cpp](https://github.com/ggml-org/whisper.cpp), with model selection
  and download, automatic language detection, local transcription cache,
  copyable results, and no mandatory cloud transcription service.
- Simplified Chinese, Traditional Chinese, and Japanese resources for the reX
  transcription interface.

### moeGramX-derived features

- Detailed message and media information: sender/chat IDs, data center, MIME
  type, file path, dimensions, duration, bitrate, and file size.
- Optional user ID/username display in place of a phone number, plus mutual
  contact and chat data-center information.
- Personal/profile channel selection and privacy controls.
- Hide reactions, unread counters, selected chat controls, input-panel
  buttons, the bottom bar, or the main chat-list section.
- High-resolution photo sending, square avatars, drawer blur/darkening, and
  further interface customization.
- Remembered message options and send-silently-by-default.
- Reduced typing-status leakage when choosing stickers.
- Save or copy photos and documents, save audio/video notes, download
  stickers, and retain FLAC cover art.

### Telegram X and repository work

- Synced with Telegram X main commit e7000d73 (version 1805).
- Current TDLib/media/build updates, baseline profiles, signing support, and
  legacy/latest Android source separation.
- API 16 through current Android releases covered by separate SDK flavors.
- Compatibility fixes for legacy variants and native Whisper builds.
- CI produces a latestArm64Release APK and publishes the rolling latest
  prerelease when signing secrets are available.
- A separate Java check covers latest ARM64 and legacy ARM32 variants.

## Integration provenance

These are the upstream points inspected for this README. Included means the
reference is an ancestor of this repository's main; feature-equivalent means
the feature was integrated through separate commits and later compatibility
fixes.

| Source | Reference | Status |
| --- | --- | --- |
| Telegram X | e7000d73, version 1805 | Included baseline |
| moeGramX moe | fda7f8bc | Included with later fixes |
| reX main | 4ac597ca | Included |
| reX musicProfile | 308d6220 | Included |
| reX split-forums | e6d439d3 | Included |
| reX split-quote | 3515c7ba | Included |
| reX topics | 16c7399f | Included |
| reX whisper | cbfa5bd9 | Included |
| reX split-music-profile | c1036cb4 | Feature-equivalent integration |
| reX split-notes | f54f712e | Feature-equivalent integration |

Notable local integration commits:

- 47a34d7e: fixes after the reX multi-branch merge.
- 5612169e: restored push configuration and repaired reX defects.
- 48cce072: local Whisper voice-message transcription.
- f8a8fd0a: profile-channel support.
- 7b5268b1: profile-note support.
- 33d6261e: Telegram X and moeGramX synchronization.
- 74ac4c50: Telegram X 1805 synchronization while preserving fork changes.

## Download

APK files are published in the rolling
[Latest Release](https://github.com/Capricornus007/moreGramX/releases/tag/latest)
and as artifacts of successful [Android CI runs](https://github.com/Capricornus007/moreGramX/actions/workflows/build.yml).
Only install builds from a source and signing key you trust. A build signed
with another key cannot update an existing installation.

## Building

### Requirements

- Linux or macOS; Windows users should use WSL.
- Git with Git LFS.
- JDK 21.
- Android SDK/NDK components selected by scripts/setup.sh.
- At least 6 GB of free disk space and 4 GB of RAM.

Clone all submodules:

~~~sh
git clone --recursive https://github.com/Capricornus007/moreGramX.git
cd moreGramX
git lfs install
~~~

If submodules were omitted:

~~~sh
git submodule update --init --recursive
~~~

Run project setup:

~~~sh
./scripts/setup.sh
~~~

Build the CI release variant without retaining a Gradle daemon:

~~~sh
./gradlew :app:assembleLatestArm64Release \
  --stacktrace --no-daemon --max-workers=2 --no-build-cache
~~~

Fast Java compatibility check:

~~~sh
./gradlew \
  :app:compileLatestArm64DebugJavaWithJavac \
  :app:compileLegacyArm32DebugJavaWithJavac \
  --stacktrace --no-daemon --max-workers=2 --no-build-cache
~~~

### SDK flavors

| Flavor | Android API range |
| --- | --- |
| legacy | 16–20 |
| lollipop | 21–22 |
| marshmallow | 23 |
| latest | 24 and newer |

### ABI flavors

arm64, arm32, x64, x86, universal, and lab are available. The primary CI
release is latestArm64.

### Signing

Local release signing uses the keystore properties expected by the build
scripts. Never commit a keystore, passwords, API hashes, Firebase
configuration belonging to another application, or other secrets.

## Updating upstreams

The conventional remotes are:

- tgx: official Telegram X main.
- moegramx: moeGramX moe.
- rex: reX and its feature branches.
- origin: this repository.

Because this project combines several long-lived forks, upstream changes must
be reviewed deliberately. Build variants, resources, TDLib APIs, native
dependencies, translations, and existing fork behavior all require checking.

## Contributing

Bug reports and focused pull requests are welcome. Include the exact version
and APK flavor, Android version and device, reproducible steps, and sanitized
logs. Preserve both current and legacy variants. Before submitting code, run
the Java checks and ARM64 release build above.

## Credits and license

- [Telegram X](https://github.com/TGX-Android/Telegram-X)
- [TDLib](https://github.com/tdlib/td)
- [moeGramX](https://github.com/moeCrafters/moeGramX)
- [reX](https://github.com/logopek/reX)
- [whisper.cpp](https://github.com/ggml-org/whisper.cpp)

moregramX is distributed under the
[GNU General Public License v3.0](LICENSE). See docs/THIRDPARTY.md and the
individual dependency license files for third-party terms.
