package com.langrunner.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.langrunner.app.exec.BinaryExecutor
import com.langrunner.app.exec.ExecOutput
import com.langrunner.app.terminal.BusyboxManager
import com.langrunner.app.terminal.ShellResult
import com.langrunner.app.terminal.ShellSession
import com.langrunner.app.terminal.StorageAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TerminalViewModel(app: Application) : AndroidViewModel(app) {

    private val busybox = BusyboxManager(app)
    private val toolboxDir = busybox.ensureInstalled()
    private val session = ShellSession(app.filesDir, toolboxDir)

    init {
        StorageAccess.ensureStorageSymlink(app.filesDir)
    }

    private val _output = MutableLiveData("")
    val output: LiveData<String> = _output

    private val _promptDir = MutableLiveData(session.currentDirectory.name.ifEmpty { "~" })
    val promptDir: LiveData<String> = _promptDir

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    private var runningProcessRef: Process? = null
    private var runningJob: Job? = null

    // Line-based buffer instead of one flat string, so \r can genuinely
    // overwrite the in-progress line (like a real terminal) instead of every
    // spinner/progress-bar frame becoming its own permanent line.
    private val completedLines = mutableListOf<String>()
    private val currentLine = StringBuilder()

    fun runBinary(binary: File, args: List<String> = emptyList()) {
        val echo = if (args.isEmpty()) binary.name else "${binary.name} ${args.joinToString(" ")}"
        appendLine("$ $echo")
        _isRunning.postValue(true)
        runningJob = viewModelScope.launch {
            try {
                BinaryExecutor.run(
                    binary,
                    args = args,
                    workingDir = session.currentDirectory,
                    env = session.env,
                    onProcess = { runningProcessRef = it }
                ).collect { handle(it) }
            } catch (e: Exception) {
                appendLine("error: ${e.message}")
            } finally {
                runningProcessRef = null
                _isRunning.postValue(false)
            }
        }
    }

    fun runCommand(command: String) {
        if (command.trim() == "clear") {
            clear()
            return
        }
        appendLine("${_promptDir.value} $ $command")
        when (val result = session.process(command)) {
            is ShellResult.Handled -> {
                if (result.output.isNotEmpty()) appendLine(result.output)
                _promptDir.postValue(session.currentDirectory.name.ifEmpty { "~" })
            }
            is ShellResult.RunExternally -> {
                _isRunning.postValue(true)
                runningJob = viewModelScope.launch {
                    try {
                        BinaryExecutor.runShellCommand(
                            result.command,
                            session.currentDirectory,
                            session.env,
                            onProcess = { runningProcessRef = it }
                        ).collect { handle(it) }
                    } catch (e: Exception) {
                        appendLine("error: ${e.message}")
                    } finally {
                        runningProcessRef = null
                        _isRunning.postValue(false)
                    }
                }
            }
        }
    }

    /** Sends typed text to the currently running process's stdin — lets
     *  Rust/etc. programs that call something like `read_line()` actually
     *  receive input, instead of the only option being to kill the process. */
    fun sendInput(text: String) {
        val process = runningProcessRef ?: return
        appendLine(text)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                process.outputStream.write((text + "\n").toByteArray(Charsets.UTF_8))
                process.outputStream.flush()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendLine("error: couldn't send input: ${e.message}")
                }
            }
        }
    }

    private fun handle(output: ExecOutput) {
        when (output) {
            is ExecOutput.Raw -> appendRaw(output.text)
            is ExecOutput.Status -> appendLine(output.text)
        }
    }

    /** Kills the currently running command — backed by its own dedicated stop button. */
    fun stopCurrentCommand() {
        runningProcessRef?.destroy()
        runningJob?.cancel()
        runningProcessRef = null
        _isRunning.postValue(false)
        appendLine("[stopped]")
    }

    /** For status/echo lines we author ourselves — always starts on a fresh line
     *  (flushes whatever was mid-progress on the current line first). */
    private fun appendLine(line: String) {
        flushCurrentLine()
        completedLines.add(line)
        publish()
    }

    /**
     * For raw process output. Processes character-by-character so `\r` acts
     * like a real terminal: it returns to the start of the current line so
     * whatever comes next overwrites it, rather than appending a new line.
     * This is what makes spinners/progress bars show as one updating line
     * instead of one permanent line per animation frame.
     */
    private fun appendRaw(text: String) {
        for (ch in text) {
            when (ch) {
                '\n' -> {
                    completedLines.add(currentLine.toString())
                    currentLine.setLength(0)
                }
                '\r' -> currentLine.setLength(0)
                else -> currentLine.append(ch)
            }
        }
        publish()
    }

    private fun flushCurrentLine() {
        if (currentLine.isNotEmpty()) {
            completedLines.add(currentLine.toString())
            currentLine.setLength(0)
        }
    }

    private fun publish() {
        val text = if (currentLine.isEmpty()) {
            completedLines.joinToString("\n")
        } else {
            (completedLines + currentLine.toString()).joinToString("\n")
        }
        _output.postValue(text)
    }

    fun clear() {
        completedLines.clear()
        currentLine.setLength(0)
        _output.postValue("")
    }
}
