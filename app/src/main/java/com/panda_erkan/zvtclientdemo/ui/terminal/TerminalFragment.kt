package com.panda_erkan.zvtclientdemo.ui.terminal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.databinding.FragmentTerminalBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class TerminalFragment : Fragment() {

    private var _binding: FragmentTerminalBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TerminalViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTerminalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
        observeViewModel()
    }

    private fun setupButtons() {
        binding.btnDiagnosis.setOnClickListener {
            viewModel.diagnosis()
        }

        binding.btnStatusEnquiry.setOnClickListener {
            viewModel.statusEnquiry()
        }

        binding.btnEndOfDay.setOnClickListener {
            viewModel.endOfDay()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressTerminal.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnDiagnosis.isEnabled = !loading
            binding.btnStatusEnquiry.isEnabled = !loading
            binding.btnEndOfDay.isEnabled = !loading
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            binding.tvTerminalStatus.text = message
        }

        viewModel.diagnosisResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            val pad = 10
            showResult(
                title = getString(R.string.diagnosis_result),
                details = buildString {
                    appendLine("${getString(R.string.label_status).padEnd(pad)}: ${if (result.success) getString(R.string.status_success) else getString(R.string.status_failed)}")
                    appendLine("${getString(R.string.label_connection).padEnd(pad)}: ${if (result.status.isConnected) getString(R.string.connection_active) else getString(R.string.connection_lost)}")
                    if (result.status.terminalId.isNotEmpty())
                        appendLine("TID".padEnd(pad) + ": ${result.status.terminalId}")
                    if (result.errorMessage.isNotEmpty())
                        appendLine("${getString(R.string.label_error).padEnd(pad)}: ${result.errorMessage}")
                }
            )
        }

        viewModel.endOfDayResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            val pad = 10
            showResult(
                title = getString(R.string.end_of_day_result),
                details = buildString {
                    appendLine("${getString(R.string.label_status).padEnd(pad)}: ${if (result.success) getString(R.string.status_success) else getString(R.string.status_failed)}")
                    appendLine("${getString(R.string.label_message).padEnd(pad)}: ${result.message}")
                    if (result.receiptLines.isNotEmpty()) {
                        appendLine("─".repeat(30))
                        result.receiptLines.forEach { appendLine(it) }
                    }
                }
            )
        }

        viewModel.terminalStatus.observe(viewLifecycleOwner) { status ->
            status ?: return@observe
            val pad = 10
            showResult(
                title = getString(R.string.terminal_status),
                details = buildString {
                    appendLine("${getString(R.string.label_connection).padEnd(pad)}: ${if (status.isConnected) getString(R.string.connection_active) else getString(R.string.connection_lost)}")
                    if (status.terminalId.isNotEmpty())
                        appendLine("TID".padEnd(pad) + ": ${status.terminalId}")
                    if (status.statusMessage.isNotEmpty())
                        appendLine("${getString(R.string.label_message).padEnd(pad)}: ${status.statusMessage}")
                }
            )
        }
    }

    private fun showResult(title: String, details: String) {
        binding.cardTerminalResult.visibility = View.VISIBLE
        binding.tvResultTitle.text = title
        binding.tvResultDetails.text = details
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
