# Lang Runner

An Android app (targeting Android 11+, API 30+) for importing and running compiled native binaries (Rust, C, C++, Go, etc.) directly on-device — no need to install the full toolchain or runtime for each language.

## Features (in progress)

- **Terminal tab** — interactive shell (`/system/bin/sh`) with scrollback output
- **Import tab** — pick any file via the system file picker, list imported binaries, and run them with one tap
- **Settings tab** — customize terminal background color, text color, font size, and font family

## How binary execution works

Since Android 10, the OS enforces W^X (write-XOR-execute): you cannot directly `exec()` a file sitting in your app's writable storage. This app routes around that the same way [Termux](https://github.com/termux/termux-exec) does — by invoking the binary through Android's dynamic linker directly:

```
/system/bin/linker64 /absolute/path/to/binary
```

The kernel only sees `linker64` (a system binary) being executed, so the W^X check passes. The linker then loads and runs your binary itself.

**Current scope:** ARM64 (`arm64-v8a`) native ELF executables only. Bytecode/interpreted languages (Python, JVM, Node) still need their runtime bundled separately — there's no universal "run any language" trick for non-native code.

## Project status

Early-stage skeleton:
- [x] 3-tab navigation (Terminal / Import / Settings)
- [x] File import via Storage Access Framework
- [x] Binary execution via the linker trick
- [x] Basic shell command execution
- [x] Customizable terminal appearance
- [ ] Full VT100/ANSI terminal emulation (cursor movement, colors, resizing)
- [ ] Bundled coreutils/busybox for a more complete shell environment
- [ ] Per-binary argument input before running

## Requirements

- Android Studio (Koala or newer recommended)
- Android SDK 34
- A physical ARM64 Android 11+ device (linker-trick execution won't work on x86 emulators without an ARM64 system image)

## Building

```
git clone https://github.com/Christianzz4603/lang-runner.git
cd lang-runner
./gradlew assembleDebug
```
