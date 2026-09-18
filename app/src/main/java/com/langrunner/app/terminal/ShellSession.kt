package com.langrunner.app.terminal

import java.io.File

sealed class ShellResult {
    data class RunExternally(val command: String) : ShellResult()
    data class Handled(val output: String) : ShellResult()
}

/**
 * Tracks state that needs to persist across commands in an interactive shell.
 * Each command runs as its own process, so something like `cd` has to be
 * intercepted and handled here rather than executed as a subprocess — a child
 * process changing its own working directory has no effect on the parent.
 */
class ShellSession(private val homeDir: File, busyboxToolboxDir: File?) {

    var currentDirectory: File = homeDir
        private set

    val env: Map<String, String> = buildMap {
        put("HOME", homeDir.absolutePath)
        put("TMPDIR", homeDir.absolutePath)
        val systemPath = "/system/bin:/system/xbin"
        put("PATH", if (busyboxToolboxDir != null) "${busyboxToolboxDir.absolutePath}:$systemPath" else systemPath)
    }

    fun process(command: String): ShellResult {
        val trimmed = command.trim()
        return when {
            trimmed.isEmpty() -> ShellResult.Handled("")
            trimmed == "cd" || trimmed == "cd ~" -> {
                currentDirectory = homeDir
                ShellResult.Handled("")
            }
            trimmed.startsWith("cd ") -> {
                val target = trimmed.removePrefix("cd ").trim()
                val resolved = if (target.startsWith("/")) File(target) else File(currentDirectory, target)
                if (resolved.isDirectory) {
                    currentDirectory = resolved
                    ShellResult.Handled("")
                } else {
                    ShellResult.Handled("cd: no such directory: $target")
                }
            }
            trimmed == "pwd" -> ShellResult.Handled(currentDirectory.absolutePath)
            else -> ShellResult.RunExternally(command)
        }
    }
}
