package com.langrunner.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.langrunner.app.exec.BinaryExecutor
import com.langrunner.app.terminal.BusyboxManager
import com.langrunner.app.terminal.ShellResult
import com.langrunner.app.terminal.ShellSession
import com.langrunner.app.terminal.StorageAccess
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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

    private val log = StringBuilder()

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
                ).collect { appendLine(it) }
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
                        ).collect { appendLine(it) }
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

    /** Kills the currently running command, if any — backs the terminal's Run/Stop toggle button. */
    fun stopCurrentCommand() {
        runningProcessRef?.destroy()
        runningJob?.cancel()
        runningProcessRef = null
        _isRunning.postValue(false)
        appendLine("[stopped]")
    }

    private fun appendLine(line: String) {
        log.append(line).append('\n')
        _output.postValue(log.toString())
    }

    fun clear() {
        log.clear()
        _output.postValue("")
    }
}
