package com.langrunner.app.exec

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.InputStreamReader

/**
 * Distinguishes raw, verbatim process output from status messages we author
 * ourselves (exit codes, error text) that should always start on their own
 * line. This split exists because real terminal output isn't line-buffered —
 * a program can print partial text with no trailing newline (progress bars,
 * prompts, spinners), and forcing every chunk of output to wait for / end in
 * a newline was mashing that kind of output together.
 */
sealed class ExecOutput {
    data class Raw(val text: String) : ExecOutput()
    data class Status(val text: String) : ExecOutput()
}

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
    private const val READ_BUFFER_SIZE = 4096

    fun run(
        binary: File,
        args: List<String> = emptyList(),
        workingDir: File,
        env: Map<String, String> = emptyMap(),
        onProcess: (Process) -> Unit = {}
    ): Flow<ExecOutput> = flow {
        if (!binary.exists()) {
            emit(ExecOutput.Status("error: file not found: ${binary.absolutePath}"))
            return@flow
        }

        val info = try {
            ElfInspector.inspect(binary)
        } catch (e: Exception) {
            emit(ExecOutput.Status("error: couldn't read '${binary.name}': ${e.message}"))
            return@flow
        }

        if (!info.isElf) {
            emit(ExecOutput.Status("error: '${binary.name}' isn't a valid ELF executable — is it actually a compiled binary?"))
            return@flow
        }

        if (!ElfInspector.isSupported(info)) {
            emit(ExecOutput.Status("error: '${binary.name}' is built for ${ElfInspector.architectureName(info.machine)}, but this app only runs ARM64 (aarch64) binaries."))
            emit(ExecOutput.Status("If this is Rust, cross-compile for Android's target instead of your PC's:"))
            emit(ExecOutput.Status("  rustup target add aarch64-linux-android"))
            emit(ExecOutput.Status("  cargo build --release --target aarch64-linux-android"))
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

            streamOutput(process, this)

            val exitCode = process.waitFor()
            emit(ExecOutput.Status("[process exited with code $exitCode]"))
        } catch (e: Exception) {
            emit(ExecOutput.Status("error: failed to run '${binary.name}': ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    /** Runs an arbitrary shell command through /system/bin/sh (used by the interactive terminal). */
    fun runShellCommand(
        command: String,
        workingDir: File,
        env: Map<String, String> = emptyMap(),
        onProcess: (Process) -> Unit = {}
    ): Flow<ExecOutput> = flow {
        try {
            val process = ProcessBuilder("/system/bin/sh", "-c", command)
                .directory(workingDir)
                .redirectErrorStream(true)
                .apply { environment().putAll(env) }
                .start()
            onProcess(process)

            streamOutput(process, this)

            val exitCode = process.waitFor()
            emit(ExecOutput.Status("[exit $exitCode]"))
        } catch (e: Exception) {
            emit(ExecOutput.Status("error: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Streams decoded output as soon as it's available, without waiting for a
     * newline — this is what lets progress bars, prompts, and no-newline
     * partial prints render the way they would in a real terminal, instead of
     * being buffered until (or garbled by) a line boundary that may never come.
     */
    private suspend fun streamOutput(process: Process, collector: FlowCollector<ExecOutput>) {
        val reader = InputStreamReader(process.inputStream, Charsets.UTF_8)
        val buffer = CharArray(READ_BUFFER_SIZE)
        while (true) {
            val read = reader.read(buffer)
            if (read == -1) break
            collector.emit(ExecOutput.Raw(String(buffer, 0, read)))
        }
    }
}
