package com.langrunner.app.model

data class ImportedFile(
    val name: String,
    val absolutePath: String,
    val sizeBytes: Long,
    val architectureLabel: String,
    val isRunnable: Boolean
)
