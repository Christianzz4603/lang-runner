package com.langrunner.app.ui

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.langrunner.app.R
import com.langrunner.app.data.SettingsRepository
import com.langrunner.app.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var settings: SettingsRepository

    private val presetColors = listOf(
        0xFF000000.toInt(), 0xFF0D1B2A.toInt(), 0xFF1B1B1B.toInt(), 0xFFFFFFFF.toInt()
    )

    private val presetTextColors = listOf(
        0xFF33FF33.toInt(), 0xFFFFFFFF.toInt(), 0xFF00BFFF.toInt(), 0xFFFFA500.toInt()
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
        presetColors.forEach { color ->
            addColorSwatch(binding.bgColorGroup, color) { settings.backgroundColor = color }
        }

        binding.textColorGroup.removeAllViews()
        presetTextColors.forEach { color ->
            addColorSwatch(binding.textColorGroup, color) { settings.textColor = color }
        }

        binding.fontSizeSlider.value = settings.fontSizeSp
        binding.fontSizeSlider.addOnChangeListener { _, value, _ ->
            settings.fontSizeSp = value
        }

        val fontAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, presetFonts)
        binding.fontFamilyDropdown.setAdapter(fontAdapter)
        binding.fontFamilyDropdown.setText(settings.fontFamily, false)
        binding.fontFamilyDropdown.setOnItemClickListener { _, _, position, _ ->
            settings.fontFamily = presetFonts[position]
        }
    }

    private fun addColorSwatch(container: LinearLayout, color: Int, onClick: () -> Unit) {
        val density = resources.displayMetrics.density
        val size = (40 * density).toInt()
        val margin = (10 * density).toInt()

        val swatch = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = margin }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                setStroke((2 * density).toInt(), ContextCompat.getColor(requireContext(), R.color.on_surface_variant))
            }
            setOnClickListener { onClick() }
        }
        container.addView(swatch)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
