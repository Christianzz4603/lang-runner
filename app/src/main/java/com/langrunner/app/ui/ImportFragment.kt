package com.langrunner.app.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.langrunner.app.MainActivity
import com.langrunner.app.databinding.FragmentImportBinding
import com.langrunner.app.exec.ElfInspector
import com.langrunner.app.model.ImportedFile
import java.io.File

class ImportFragment : Fragment() {

    private var _binding: FragmentImportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TerminalViewModel by activityViewModels()
    private lateinit var adapter: ImportedFileAdapter
    private lateinit var binDir: File

    private val pickFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { copyIntoAppStorage(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binDir = File(requireContext().filesDir, "bin").apply { mkdirs() }

        adapter = ImportedFileAdapter { imported ->
            showArgsDialog(File(imported.absolutePath))
        }

        binding.importedList.layoutManager = LinearLayoutManager(requireContext())
        binding.importedList.adapter = adapter

        binding.importButton.setOnClickListener {
            pickFileLauncher.launch(arrayOf("*/*"))
        }

        refreshList()
    }

    /** e.g. running an imported curl binary needs a URL/flags — ask before launching. */
    private fun showArgsDialog(binary: File) {
        val input = EditText(requireContext()).apply {
            hint = "e.g. -O https://example.com/file"
            setSingleLine()
        }
        val padding = (16 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(requireContext()).apply {
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Run ${binary.name}")
            .setMessage("Arguments (optional)")
            .setView(container)
            .setPositiveButton("Run") { _, _ ->
                val args = input.text.toString().trim()
                    .split(Regex("\\s+"))
                    .filter { it.isNotEmpty() }
                viewModel.runBinary(binary, args)
                (requireActivity() as? MainActivity)?.switchToTerminalTab()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun copyIntoAppStorage(uri: Uri) {
        val resolver = requireContext().contentResolver
        val displayName = queryDisplayName(uri) ?: "imported_${System.currentTimeMillis()}"
        val destination = File(binDir, displayName)

        try {
            resolver.openInputStream(uri)?.use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destination.setExecutable(true, false)
            destination.setReadable(true, false)
        } catch (_: Exception) {
            // Surfaced to the user via the file simply not appearing / size showing 0 in the list.
        }
        refreshList()
    }

    private fun queryDisplayName(uri: Uri): String? {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex >= 0) {
                return it.getString(nameIndex)
            }
        }
        return null
    }

    private fun refreshList() {
        val files = binDir.listFiles()?.map { file ->
            val info = try {
                ElfInspector.inspect(file)
            } catch (_: Exception) {
                ElfInspector.ElfInfo(isElf = false, is64Bit = false, machine = 0)
            }
            val label = if (!info.isElf) "not an ELF executable" else ElfInspector.architectureName(info.machine)
            ImportedFile(
                name = file.name,
                absolutePath = file.absolutePath,
                sizeBytes = file.length(),
                architectureLabel = label,
                isRunnable = ElfInspector.isSupported(info)
            )
        } ?: emptyList()
        adapter.submitList(files)
        binding.emptyState.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
