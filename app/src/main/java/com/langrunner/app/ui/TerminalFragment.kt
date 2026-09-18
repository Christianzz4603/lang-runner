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
import com.langrunner.app.R
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
            binding.terminalSend.setIconResource(if (running) R.drawable.ic_stop else R.drawable.ic_run)
        }

        binding.terminalSend.setOnClickListener {
            if (viewModel.isRunning.value == true) {
                viewModel.stopCurrentCommand()
            } else {
                sendCommand()
            }
        }

        // IMPORTANT: only react to the actual IME "send" action. Some keyboards
        // invoke this listener a second time with a raw Enter KeyEvent for the
        // same press — reacting to that too was submitting every command twice.
        binding.terminalInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCommand()
                true
            } else {
                false
            }
        }
    }

    private fun sendCommand() {
        val cmd = binding.terminalInput.text.toString().trim()
        if (cmd.isNotEmpty()) {
            viewModel.runCommand(cmd)
            binding.terminalInput.text.clear()
        }
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
