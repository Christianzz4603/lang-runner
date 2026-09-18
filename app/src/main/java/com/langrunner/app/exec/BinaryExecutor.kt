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
 * The kernel's writable-file exec check only inspects the direct execve() target;
 * since we execve() the system linker (not our own file) and it loads/mmaps the
 * target ELF itself, the restriction doesn't apply.
 *
 * Every failure mode here (wrong architecture, bad/corrupt file, permission
 * issues, the process itself failing to start) is caught and turned into an
 * error line instead of being allowed to crash the host app.
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

        val info = try {
            ElfInspector.inspect(binary)
        } catch (e: Exception) {
            emit("error: couldn't read '${binary.name}': ${e.message}")
            return@flow
        }

        if (!info.isElf) {
            emit("error: '${binary.name}' isn't a valid ELF executable — is it actually a compiled binary?")
            return@flow
        }

        if (!ElfInspector.isSupported(info)) {
            emit("error: '${binary.name}' is built for ${ElfInspector.architectureName(info.machine)}, but this app only runs ARM64 (aarch64) binaries.")
            emit("If this is Rust, cross-compile for Android's target instead of your PC's:")
            emit("  rustup target add aarch64-linux-android")
            emit("  cargo build --release --target aarch64-linux-android")
            return@flow
        }

        try {
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
        } catch (e: Exception) {
            emit("error: failed to run '${binary.name}': ${e.message}")
        }
    }.flowOn(Dispatchers.IO)

    /** Runs an arbitrary shell command through /system/bin/sh (used by the interactive terminal). */
    fun runShellCommand(
        command: String,
        workingDir: File,
        env: Map<String, String> = emptyMap(),
        onProcess: (Process) -> Unit = {}
    ): Flow<String> = flow {
        try {
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
        } catch (e: Exception) {
            emit("error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO)
}
