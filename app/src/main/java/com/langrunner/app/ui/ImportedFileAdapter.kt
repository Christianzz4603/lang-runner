package com.langrunner.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.langrunner.app.databinding.ItemImportedFileBinding
import com.langrunner.app.model.ImportedFile

class ImportedFileAdapter(
    private val onRunClicked: (ImportedFile) -> Unit
) : RecyclerView.Adapter<ImportedFileAdapter.ViewHolder>() {

    private val items = mutableListOf<ImportedFile>()

    fun submitList(newItems: List<ImportedFile>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImportedFileBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemImportedFileBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ImportedFile) {
            binding.fileName.text = item.name
            binding.fileSize.text = "${item.sizeBytes / 1024} KB · ${item.architectureLabel}"
            binding.runButton.isEnabled = item.isRunnable
            binding.runButton.alpha = if (item.isRunnable) 1f else 0.4f
            binding.runButton.text = if (item.isRunnable) "Run" else "Unsupported"
            binding.runButton.setOnClickListener {
                if (item.isRunnable) onRunClicked(item)
            }
        }
    }
}
