package com.langrunner.app.ui

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.langrunner.app.data.SettingsRepository
import com.langrunner.app.databinding.FragmentTerminalBinding
import com.langrunner.app.terminal.AnsiParser

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
            binding.terminalOutput.text = AnsiParser.render(text, settings.textColor)
            binding.terminalScroll.post {
                binding.terminalScroll.fullScroll(View.FOCUS_DOWN)
            }
        }

        viewModel.promptDir.observe(viewLifecycleOwner) { dirName ->
            binding.promptLabel.text = "$dirName $"
        }

        viewModel.isRunning.observe(viewLifecycleOwner) { running ->
            binding.runningIndicator.visibility = if (running) View.VISIBLE else View.GONE
            binding.stopButton.visibility = if (running) View.VISIBLE else View.GONE
            binding.terminalInput.hint = if (running) "send input to running process…" else "command"
        }

        // Send always submits whatever's typed: to the running process's stdin
        // if something's running, otherwise as a new command. Stopping a
        // process is a separate, deliberate action (the red stop button) so
        // it can never happen by accident while trying to send input.
        binding.terminalSend.setOnClickListener { submit() }
        binding.terminalInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submit()
                true
            } else {
                false
            }
        }

        binding.stopButton.setOnClickListener { viewModel.stopCurrentCommand() }
    }

    private fun submit() {
        val text = binding.terminalInput.text.toString()
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        if (viewModel.isRunning.value == true) {
            viewModel.sendInput(text)
        } else {
            viewModel.runCommand(trimmed)
        }
        binding.terminalInput.text.clear()
    }

    private fun applyAppearance() {
        binding.terminalCard.setCardBackgroundColor(settings.backgroundColor)
        binding.terminalOutput.setTextSize(TypedValue.COMPLEX_UNIT_SP, settings.fontSizeSp)
        binding.terminalOutput.typeface = Typeface.create(settings.fontFamily, Typeface.NORMAL)
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
