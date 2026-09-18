package com.langrunner.app.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.langrunner.app.exec.BinaryExecutor
import kotlinx.coroutines.launch
import java.io.File

class TerminalViewModel : ViewModel() {

    private val _output = MutableLiveData("")
    val output: LiveData<String> = _output

    private val log = StringBuilder()

    fun runBinary(binary: File, workingDir: File) {
        appendLine("$ ${binary.name}")
        viewModelScope.launch {
            BinaryExecutor.run(binary, workingDir = workingDir).collect { line ->
                appendLine(line)
            }
        }
    }

    fun runCommand(command: String, workingDir: File) {
        appendLine("$ $command")
        viewModelScope.launch {
            BinaryExecutor.runShellCommand(command, workingDir).collect { line ->
                appendLine(line)
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
