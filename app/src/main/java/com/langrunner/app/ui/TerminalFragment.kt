package com.langrunner.app.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.langrunner.app.data.SettingsRepository
import com.langrunner.app.databinding.FragmentTerminalBinding

class TerminalFragment : Fragment() {

    private var _binding: FragmentTerminalBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TerminalViewModel by activityViewModels()
    private lateinit var settings: SettingsRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTerminalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = SettingsRepository(requireContext())
        applyAppearance()

        viewModel.output.observe(viewLifecycleOwner) { text ->
            binding.terminalOutput.text = text
            binding.terminalScroll.post {
                binding.terminalScroll.fullScroll(View.FOCUS_DOWN)
            }
        }

        binding.terminalSend.setOnClickListener {
            val cmd = binding.terminalInput.text.toString().trim()
            if (cmd.isNotEmpty()) {
                val workingDir = requireContext().filesDir
                viewModel.runCommand(cmd, workingDir)
                binding.terminalInput.text.clear()
            }
        }
    }

    private fun applyAppearance() {
        binding.terminalOutput.setBackgroundColor(settings.backgroundColor)
        binding.terminalOutput.setTextColor(settings.textColor)
        binding.terminalOutput.textSize = settings.fontSizeSp
        binding.terminalOutput.typeface = Typeface.create(settings.fontFamily, Typeface.NORMAL)
        binding.root.setBackgroundColor(settings.backgroundColor)
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) applyAppearance()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
