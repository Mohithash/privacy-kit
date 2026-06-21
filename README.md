# Privacy Kit

An Android privacy / anti-detection module for LSPosed / Vector, built fresh from scratch.

## Status: early scaffold

This is a clean rewrite, not a fork. The previous iteration (a heavily modified fork of
[0bbedCode/XPL-EX](https://github.com/0bbedCode/XPL-EX), itself based on
[XPrivacyLua](https://github.com/M66B/XPrivacyLua)) accumulated enough legacy architecture
and dead code that a fresh start made more sense than continuing to retrofit it.

Right now this repo contains a verified-building scaffold only:
- A classic Xposed API entry point (`assets/xposed_init` → `XposedEntry.kt`) that logs on
  load — no real hooks yet
- A placeholder Jetpack Compose screen
- Build configuration only (Kotlin 2.2.20, AGP 8.9.1, Compose, compileSdk/targetSdk 36)

## Architecture decisions

- **Kotlin**, not Java
- **Classic `de.robv.android.xposed` API**, not the newer `io.github.libxposed` API —
  every real-world module surveyed before starting this rewrite ships its actual hooks on
  the classic API regardless of what else it touches, and it's fully supported by
  LSPosed/Vector via a compatibility layer
- **JSON + Lua hook definitions** (data-driven hooks, addable without recompiling) via
  [`org.luaj:luaj-jse`](https://central.sonatype.com/artifact/org.luaj/luaj-jse) as a real
  Maven dependency, not vendored source
- **Jetpack Compose + Material 3** for the UI

## Building

```
./gradlew assembleDebug
```

Requires JDK 17 and the Android SDK with API 36 platform + matching build-tools installed.

## Roadmap

- Port detection/spoofing logic (root detection bypass, emulator detection bypass, device
  fingerprint spoofing, location spoofing, Play Integrity interception)
- Build the hook engine (JSON hook definitions + Lua script bridge + install loop)
- Per-app hook assignment UI
- Hook diagnostics (which hooks actually installed vs. failed to resolve on a given device)
- Privacy report (which hooks fired, with what result)
