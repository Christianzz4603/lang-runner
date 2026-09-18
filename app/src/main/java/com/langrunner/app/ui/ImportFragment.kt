package com.langrunner.app.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.langrunner.app.MainActivity
import com.langrunner.app.databinding.FragmentImportBinding
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
            val binary = File(imported.absolutePath)
            viewModel.runBinary(binary, workingDir = binDir)
            (requireActivity() as? MainActivity)?.switchToTerminalTab()
        }

        binding.importedList.layoutManager = LinearLayoutManager(requireContext())
        binding.importedList.adapter = adapter

        binding.importButton.setOnClickListener {
            pickFileLauncher.launch(arrayOf("*/*"))
        }

        refreshList()
    }

    private fun copyIntoAppStorage(uri: Uri) {
        val resolver = requireContext().contentResolver
        val displayName = queryDisplayName(uri) ?: "imported_${System.currentTimeMillis()}"
        val destination = File(binDir, displayName)

        resolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        destination.setExecutable(true, false)
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
        val files = binDir.listFiles()?.map {
            ImportedFile(name = it.name, absolutePath = it.absolutePath, sizeBytes = it.length())
        } ?: emptyList()
        adapter.submitList(files)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
