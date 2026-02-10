package com.panda_erkan.zvtclientdemo.ui.terminal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.panda.zvt_library.model.ConnectionState
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.databinding.FragmentTerminalBinding
import com.panda_erkan.zvtclientdemo.ui.common.ProgressStatusDialog
import org.koin.androidx.viewmodel.ext.android.viewModel

class TerminalFragment : Fragment() {

    private var _binding: FragmentTerminalBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TerminalViewModel by viewModel()
    private var progressDialog: ProgressStatusDialog? = null
    private var currentOperationName: String = ""
    private var currentOperationIcon: String = ""

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
            setOperation(getString(R.string.op_diagnosis), "\uD83D\uDCCA")
            viewModel.diagnosis()
        }

        binding.btnStatusEnquiry.setOnClickListener {
            setOperation(getString(R.string.op_status_enquiry), "\uD83D\uDD0D")
            viewModel.statusEnquiry()
        }

        binding.btnEndOfDay.setOnClickListener {
            setOperation(getString(R.string.op_end_of_day), "\uD83D\uDCC5")
            viewModel.endOfDay()
        }

        binding.btnRepeatReceipt.setOnClickListener {
            setOperation(getString(R.string.op_repeat_receipt), "\uD83D\uDDA8\uFE0F")
            viewModel.repeatReceipt()
        }

        binding.btnLogOff.setOnClickListener {
            setOperation(getString(R.string.op_log_off), "\uD83D\uDEAA")
            viewModel.logOff()
        }
    }

    private fun setOperation(name: String, icon: String) {
        currentOperationName = name
        currentOperationIcon = icon
    }

    private fun showProgressDialog() {
        if (progressDialog?.isAdded == true) return
        progressDialog = ProgressStatusDialog.newInstance(
            operationName = currentOperationName,
            operationIcon = currentOperationIcon
        )
        progressDialog?.show(childFragmentManager, ProgressStatusDialog.TAG)
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismissAllowingStateLoss()
        progressDialog = null
    }

    private fun updateButtonStates() {
        val registered = viewModel.connectionState.value == ConnectionState.REGISTERED
        val loading = viewModel.isLoading.value == true
        val enabled = registered && !loading

        binding.btnDiagnosis.isEnabled = enabled
        binding.btnStatusEnquiry.isEnabled = enabled
        binding.btnEndOfDay.isEnabled = enabled
        binding.btnRepeatReceipt.isEnabled = enabled
        binding.btnLogOff.isEnabled = enabled
        binding.progressTerminal.visibility = if (loading) View.VISIBLE else View.GONE
    }

    private fun observeViewModel() {
        viewModel.connectionState.observe(viewLifecycleOwner) { updateButtonStates() }
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            updateButtonStates()
            if (loading == true) {
                showProgressDialog()
            } else {
                dismissProgressDialog()
            }
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            binding.tvTerminalStatus.text = message
            // Update dialog status too
            if (message.isNotEmpty()) {
                progressDialog?.updateStatus(message)
            }
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

        viewModel.repeatReceiptResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            val pad = 10
            showResult(
                title = getString(R.string.repeat_receipt_result),
                details = buildString {
                    appendLine("${getString(R.string.label_status).padEnd(pad)}: ${if (result.success) getString(R.string.status_success) else getString(R.string.status_failed)}")
                    appendLine("${getString(R.string.label_message).padEnd(pad)}: ${result.resultMessage}")
                    if (result.receiptLines.isNotEmpty()) {
                        appendLine("─".repeat(30))
                        result.receiptLines.forEach { appendLine(it) }
                    }
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
