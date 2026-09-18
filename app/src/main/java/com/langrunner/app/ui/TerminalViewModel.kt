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
import kotlinx.coroutines.launch
import java.io.File

class TerminalViewModel(app: Application) : AndroidViewModel(app) {

    private val busybox = BusyboxManager(app)
    private val toolboxDir = busybox.ensureInstalled()
    private val session = ShellSession(app.filesDir, toolboxDir)

    private val _output = MutableLiveData("")
    val output: LiveData<String> = _output

    private val _promptDir = MutableLiveData(session.currentDirectory.name.ifEmpty { "~" })
    val promptDir: LiveData<String> = _promptDir

    private val log = StringBuilder()

    fun runBinary(binary: File) {
        appendLine("$ ${binary.name}")
        viewModelScope.launch {
            BinaryExecutor.run(binary, workingDir = session.currentDirectory, env = session.env)
                .collect { appendLine(it) }
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
                viewModelScope.launch {
                    BinaryExecutor.runShellCommand(result.command, session.currentDirectory, session.env)
                        .collect { appendLine(it) }
                }
            }
        }
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
