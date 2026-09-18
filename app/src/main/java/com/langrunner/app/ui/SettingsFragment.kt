package com.langrunner.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.langrunner.app.data.SettingsRepository
import com.langrunner.app.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var settings: SettingsRepository

    private val presetColors = listOf(
        0xFF000000.toInt() to "Black",
        0xFF0D1B2A.toInt() to "Navy",
        0xFF1B1B1B.toInt() to "Charcoal",
        0xFFFFFFFF.toInt() to "White"
    )

    private val presetTextColors = listOf(
        0xFF33FF33.toInt() to "Matrix Green",
        0xFFFFFFFF.toInt() to "White",
        0xFF00BFFF.toInt() to "Cyan",
        0xFFFFA500.toInt() to "Amber"
    )

    private val presetFonts = listOf("monospace", "sans-serif", "serif")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = SettingsRepository(requireContext())

        binding.bgColorGroup.removeAllViews()
        presetColors.forEach { (color, label) ->
            addColorChip(binding.bgColorGroup, color, label) { settings.backgroundColor = color }
        }

        binding.textColorGroup.removeAllViews()
        presetTextColors.forEach { (color, label) ->
            addColorChip(binding.textColorGroup, color, label) { settings.textColor = color }
        }

        binding.fontSizeSlider.value = settings.fontSizeSp
        binding.fontSizeSlider.addOnChangeListener { _, value, _ ->
            settings.fontSizeSp = value
        }

        val fontAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            presetFonts
        )
        binding.fontFamilySpinner.adapter = fontAdapter
        val currentIndex = presetFonts.indexOf(settings.fontFamily).coerceAtLeast(0)
        binding.fontFamilySpinner.setSelection(currentIndex)
        binding.fontFamilySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                settings.fontFamily = presetFonts[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun addColorChip(
        container: android.widget.LinearLayout,
        color: Int,
        label: String,
        onClick: () -> Unit
    ) {
        val chip = MaterialButton(requireContext()).apply {
            text = label
            setBackgroundColor(color)
            setOnClickListener { onClick() }
        }
        container.addView(chip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
