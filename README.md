# Lang Runner

![Android CI](https://github.com/Christianzz4603/lang-runner/actions/workflows/android-ci.yml/badge.svg)

An Android app (targeting Android 11+, API 30+) for importing and running compiled native binaries (Rust, C, C++, Go, etc.) directly on-device — no need to install the full toolchain or runtime for each language.

## Features

- **Terminal tab** — interactive shell with a persistent working directory (`cd`/`pwd` work as expected), ANSI color rendering, dark card-based Material 3 UI
- **Import tab** — pick any file via the system file picker, see imported binaries as cards, run them with one tap (jumps to Terminal automatically)
- **Settings tab** — background/text color swatches, font size slider, font family picker

## How binary execution works

Since Android 10, the OS enforces W^X (write-XOR-execute): you cannot directly `exec()` a file sitting in your app's writable storage. This app routes around that the same way [Termux](https://github.com/termux/termux-exec) does — by invoking the binary through Android's dynamic linker directly:

```
/system/bin/linker64 /absolute/path/to/binary
```

The kernel only sees `linker64` (a system binary) being executed, so the W^X check passes. The linker then loads and runs your binary itself.

Bundled tools (like busybox) use a different, more direct mechanism: files packaged under `jniLibs/<abi>/` with a `lib*.so` name get extracted with real execute permission at install time, so they can be `exec()`'d directly — no linker trick needed. See `app/src/main/jniLibs/README.md`.

**Current scope:** ARM64 (`arm64-v8a`) native ELF executables only. Bytecode/interpreted languages (Python, JVM, Node) still need their runtime bundled separately — there's no universal "run any language" trick for non-native code.

## Project status

- [x] 3-tab navigation, Material 3 dark UI (cards, FAB, custom icons)
- [x] File import via Storage Access Framework
- [x] Binary execution via the linker trick
- [x] Persistent working directory (`cd`, `pwd` handled properly across commands)
- [x] ANSI color/style rendering (SGR codes — colored build output, `ls --color`, etc.)
- [x] Busybox integration wiring (symlinks + PATH) — **requires you to add the actual binary**, see `app/src/main/jniLibs/README.md`
- [x] GitHub Actions CI (builds a debug APK on every push)
- [ ] Full cursor-addressable VT100 terminal emulation (cursor positioning, alternate screen buffer, resizing) — needed for full-screen programs like `vim` or `htop`
- [ ] Per-binary argument input before running

## Requirements

- Android Studio (Koala or newer recommended)
- Android SDK 34
- A physical ARM64 Android 11+ device (linker-trick execution won't work on x86 emulators without an ARM64 system image)

## Building locally

```
git clone https://github.com/Christianzz4603/lang-runner.git
cd lang-runner
./gradlew assembleDebug
```

CI (`.github/workflows/android-ci.yml`) builds and lints on every push to `main` and uploads the debug APK as a workflow artifact.
