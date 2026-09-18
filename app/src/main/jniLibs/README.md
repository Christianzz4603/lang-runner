This app does not ship an actual busybox binary — it's a ~1-2MB prebuilt static
executable that has to be downloaded (or built) separately and added by hand.

Steps:

1. Download a static ARM64 (aarch64) busybox build, e.g. from
   https://busybox.net/downloads/binaries/ (pick the arm64/aarch64 static build)
   or build one yourself with the Android NDK.
2. Rename it to `libbusybox.so`.
3. Place it at: `app/src/main/jniLibs/arm64-v8a/libbusybox.so`

Why the `lib*.so` name and this exact folder: Android's packager only grants
automatic execute permission (and, with `extractNativeLibs="true"", extracts
as a real file the app can `exec()`) to files that live under `jniLibs/<abi>/`
and match the `lib*.so` naming convention — even though the content here is a
normal ELF executable, not a real shared library. This is the same mechanism
Termux and similar apps use to ship bundled toolchains without root.

Once it's in place, `BusyboxManager` (see
`app/src/main/java/com/langrunner/app/terminal/BusyboxManager.kt`) will
automatically detect it, create applet symlinks in the app's private storage,
and put them on PATH ahead of `/system/bin` for the interactive shell.
