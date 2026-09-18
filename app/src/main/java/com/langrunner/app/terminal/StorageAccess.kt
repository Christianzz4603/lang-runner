package com.langrunner.app.terminal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.system.ErrnoException
import android.system.Os
import java.io.File

/**
 * Mirrors Termux's `termux-setup-storage`. Android 11+ sandboxes apps to
 * their own private storage by default — reaching shared storage (e.g.
 * Downloads, a Rust binary reading/writing arbitrary paths) requires the
 * user to explicitly grant "All files access" (MANAGE_EXTERNAL_STORAGE),
 * which can't be granted through a normal runtime permission dialog and has
 * to be requested via a system settings screen instead.
 *
 * Once granted, this creates a `~/storage/shared` symlink to the shared
 * storage root — the same convention Termux uses — so scripts/binaries have
 * a stable, predictable path.
 */
object StorageAccess {

    fun hasAllFilesAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }

    fun requestAllFilesAccessIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    /** Idempotent — safe to call on every app start. No-ops until access is granted. */
    fun ensureStorageSymlink(homeDir: File): File? {
        if (!hasAllFilesAccess()) return null

        val storageDir = File(homeDir, "storage").apply { mkdirs() }
        val sharedLink = File(storageDir, "shared")
        if (!sharedLink.exists()) {
            try {
                Os.symlink(Environment.getExternalStorageDirectory().absolutePath, sharedLink.absolutePath)
            } catch (_: ErrnoException) {
                // Already exists or filesystem doesn't support symlinks here — non-fatal.
            }
        }
        return storageDir
    }
}
