package com.langrunner.app.terminal

import android.content.Context
import android.system.ErrnoException
import android.system.Os
import java.io.File

/**
 * Wires up a bundled busybox binary (shipped as a native library so the OS
 * grants it exec permission automatically on install — no linker trick
 * needed for binaries packaged with the APK itself). Creates one symlink per
 * applet name in a private "toolbox" directory so busybox's argv[0]-based
 * applet dispatch works, and that directory is put on PATH ahead of
 * /system/bin.
 *
 * Includes wget (bundled in busybox itself) for basic downloads. curl is a
 * separate project, not part of busybox — see the Import tab for running a
 * separately-downloaded static curl binary instead.
 *
 * This project does not ship an actual busybox binary — see
 * app/src/main/jniLibs/README.md for how to add one. Everything here
 * degrades gracefully (isAvailable == false, PATH falls back to /system/bin)
 * until that file is added.
 */
class BusyboxManager(private val context: Context) {

    private val applets = listOf(
        "ls", "cat", "echo", "grep", "sed", "awk", "find", "ps", "mkdir", "rm",
        "mv", "cp", "chmod", "tar", "gzip", "gunzip", "wc", "head", "tail",
        "sort", "uniq", "cut", "diff", "du", "df", "which", "xargs",
        "wget", "nc"
    )

    val toolboxDir: File = File(context.filesDir, "toolbox")

    val busyboxBinary: File?
        get() {
            val candidate = File(context.applicationInfo.nativeLibraryDir, "libbusybox.so")
            return if (candidate.exists()) candidate else null
        }

    val isAvailable: Boolean get() = busyboxBinary != null

    /** Idempotent — safe to call on every app start. Returns the toolbox dir, or null if busybox isn't bundled. */
    fun ensureInstalled(): File? {
        val busybox = busyboxBinary ?: return null
        toolboxDir.mkdirs()

        for (applet in applets) {
            val link = File(toolboxDir, applet)
            if (!link.exists()) {
                try {
                    Os.symlink(busybox.absolutePath, link.absolutePath)
                } catch (_: ErrnoException) {
                    // Already exists or filesystem doesn't support symlinks here — skip.
                }
            }
        }
        return toolboxDir
    }
}
