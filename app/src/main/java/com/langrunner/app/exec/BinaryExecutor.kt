package com.langrunner.app.exec

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Runs imported ARM64 ELF binaries by invoking Android's dynamic linker directly
 * (`/system/bin/linker64 <path>`), which routes around the W^X restriction that
 * blocks exec() on files inside the app's writable storage (Android 10+).
 *
 * This is the same technique termux-exec uses: https://github.com/termux/termux-exec
 * It works for binaries that use standard libc exec paths; programs relying on raw
 * execve() syscalls internally would need to be patched or run under an interpreter.
 */
object BinaryExecutor {

    private const val LINKER64 = "/system/bin/linker64"

    fun run(
        binary: File,
        args: List<String> = emptyList(),
        workingDir: File,
        env: Map<String, String> = emptyMap(),
        onProcess: (Process) -> Unit = {}
    ): Flow<String> = flow {
        if (!binary.exists()) {
            emit("error: file not found: ${binary.absolutePath}")
            return@flow
        }

        val command = mutableListOf(LINKER64, binary.absolutePath)
        command.addAll(args)

        val process = ProcessBuilder(command)
            .directory(workingDir)
            .redirectErrorStream(true)
            .apply { environment().putAll(env) }
            .start()
        onProcess(process)

        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (true) {
                line = reader.readLine() ?: break
                emit(line)
            }
        }

        val exitCode = process.waitFor()
        emit("[process exited with code $exitCode]")
    }.flowOn(Dispatchers.IO)

    /** Runs an arbitrary shell command through /system/bin/sh (used by the interactive terminal). */
    fun runShellCommand(
        command: String,
        workingDir: File,
        env: Map<String, String> = emptyMap(),
        onProcess: (Process) -> Unit = {}
    ): Flow<String> = flow {
        val process = ProcessBuilder("/system/bin/sh", "-c", command)
            .directory(workingDir)
            .redirectErrorStream(true)
            .apply { environment().putAll(env) }
            .start()
        onProcess(process)

        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (true) {
                line = reader.readLine() ?: break
                emit(line)
            }
        }

        val exitCode = process.waitFor()
        emit("[exit $exitCode]")
    }.flowOn(Dispatchers.IO)
}
