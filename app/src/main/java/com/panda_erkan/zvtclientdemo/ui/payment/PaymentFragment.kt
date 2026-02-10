package com.panda_erkan.zvtclientdemo.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.panda.zvt_library.model.ConnectionState
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.databinding.FragmentPaymentBinding
import com.panda_erkan.zvtclientdemo.ui.common.ProgressStatusDialog
import org.koin.androidx.viewmodel.ext.android.viewModel

class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PaymentViewModel by viewModel()
    private var progressDialog: ProgressStatusDialog? = null
    private var currentOperationName: String = ""
    private var currentOperationIcon: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
        observeViewModel()
    }

    private fun setupButtons() {
        binding.btnAuthorize.setOnClickListener {
            val amount = binding.etAmount.text.toString()
            if (amount.isNotEmpty()) {
                setOperation(getString(R.string.op_payment), "\uD83D\uDCB3")
                viewModel.authorize(amount)
            } else {
                binding.etAmount.error = getString(R.string.error_amount_required)
            }
        }

        binding.btnRefund.setOnClickListener {
            val amount = binding.etAmount.text.toString()
            if (amount.isNotEmpty()) {
                setOperation(getString(R.string.op_refund), "\uD83D\uDD04")
                viewModel.refund(amount)
            } else {
                binding.etAmount.error = getString(R.string.error_amount_required)
            }
        }

        binding.btnReversal.setOnClickListener {
            val receiptNo = binding.etReceiptNumber.text.toString().toIntOrNull()
            setOperation(getString(R.string.op_reversal), "\u21A9\uFE0F")
            viewModel.reversal(receiptNo)
        }

        binding.btnAbort.setOnClickListener {
            viewModel.abort()
        }

        binding.btnPreAuth.setOnClickListener {
            val amount = binding.etAmount.text.toString()
            if (amount.isNotEmpty()) {
                setOperation(getString(R.string.op_pre_auth), "\uD83D\uDD12")
                viewModel.preAuthorize(amount)
            } else {
                binding.etAmount.error = getString(R.string.error_amount_required)
            }
        }

        binding.btnBookTotal.setOnClickListener {
            val amount = binding.etAmount.text.toString()
            val receiptNo = binding.etReceiptNumber.text.toString().toIntOrNull()
            if (amount.isEmpty()) {
                binding.etAmount.error = getString(R.string.error_amount_required)
            } else if (receiptNo == null) {
                binding.etReceiptNumber.error = getString(R.string.receipt_no_required)
            } else {
                setOperation(getString(R.string.op_book_total), "\u2705")
                viewModel.bookTotal(amount, receiptNo)
            }
        }

        binding.btnPartialReversal.setOnClickListener {
            val amount = binding.etAmount.text.toString()
            val receiptNo = binding.etReceiptNumber.text.toString().toIntOrNull()
            if (amount.isEmpty()) {
                binding.etAmount.error = getString(R.string.error_amount_required)
            } else if (receiptNo == null) {
                binding.etReceiptNumber.error = getString(R.string.receipt_no_required)
            } else {
                setOperation(getString(R.string.op_partial_reversal), "\u2702\uFE0F")
                viewModel.partialReversal(amount, receiptNo)
            }
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
            operationIcon = currentOperationIcon,
            onCancel = { viewModel.abort() }
        )
        progressDialog?.show(childFragmentManager, ProgressStatusDialog.TAG)
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismissAllowingStateLoss()
        progressDialog = null
    }

    private fun updateButtonStates() {
        val registered = viewModel.connectionState.value == ConnectionState.REGISTERED
        val processing = viewModel.isProcessing.value == true
        val enabled = registered && !processing

        binding.btnAuthorize.isEnabled = enabled
        binding.btnRefund.isEnabled = enabled
        binding.btnReversal.isEnabled = enabled
        binding.btnPreAuth.isEnabled = enabled
        binding.btnBookTotal.isEnabled = enabled
        binding.btnPartialReversal.isEnabled = enabled
        // Abort is always enabled when connected (to cancel running operations)
        binding.btnAbort.isEnabled = registered
    }

    private fun observeViewModel() {
        viewModel.connectionState.observe(viewLifecycleOwner) { updateButtonStates() }
        viewModel.isProcessing.observe(viewLifecycleOwner) { processing ->
            updateButtonStates()
            if (processing == true) {
                showProgressDialog()
            }
            // Dialog is dismissed via showResult() auto-dismiss, not here
        }

        viewModel.intermediateStatus.observe(viewLifecycleOwner) { status ->
            if (status.isNullOrEmpty()) {
                binding.cardIntermediateStatus.visibility = View.GONE
            } else {
                binding.cardIntermediateStatus.visibility = View.VISIBLE
                binding.tvIntermediateStatus.text = status
                progressDialog?.updateStatus(status)
            }
        }

        viewModel.transactionResult.observe(viewLifecycleOwner) { result ->
            if (result == null) {
                binding.cardResult.visibility = View.GONE
                return@observe
            }

            // Build detailed result for the popup dialog
            val resultMsg = if (result.success) {
                getString(R.string.transaction_success)
            } else {
                result.resultMessage
            }
            val pad = 12
            val detailsForDialog = buildString {
                appendLine("${getString(R.string.label_amount).padEnd(pad)}: ${result.amountFormatted}")
                appendLine("${getString(R.string.label_result).padEnd(pad)}: ${result.resultMessage}")
                if (result.traceNumber > 0) appendLine("${getString(R.string.label_trace_no).padEnd(pad)}: ${result.traceNumber}")
                if (result.receiptNumber > 0) appendLine("${getString(R.string.label_receipt_no).padEnd(pad)}: ${result.receiptNumber}")
                if (result.terminalId.isNotEmpty()) appendLine("${getString(R.string.label_terminal).padEnd(pad)}: ${result.terminalId}")
                if (result.vuNumber.isNotEmpty()) appendLine("${getString(R.string.label_vu_number).padEnd(pad)}: ${result.vuNumber}")
                result.cardData?.let { card ->
                    appendLine("\u2500".repeat(30))
                    if (card.cardType.isNotEmpty()) appendLine("${getString(R.string.label_card_type).padEnd(pad)}: ${card.cardType}")
                    if (card.maskedPan.isNotEmpty()) appendLine("${getString(R.string.label_card_no).padEnd(pad)}: ${card.maskedPan}")
                    if (card.cardName.isNotEmpty()) appendLine("${getString(R.string.label_card_name).padEnd(pad)}: ${card.cardName}")
                    if (card.expiryDate.isNotEmpty()) appendLine("${getString(R.string.label_expiry).padEnd(pad)}: ${card.expiryDate}")
                    if (card.sequenceNumber > 0) appendLine("${getString(R.string.label_seq_no).padEnd(pad)}: ${card.sequenceNumber}")
                    if (card.aid.isNotEmpty()) appendLine("${getString(R.string.label_aid).padEnd(pad)}: ${card.aid}")
                }
                if (result.date.isNotEmpty()) {
                    appendLine("\u2500".repeat(30))
                    appendLine("${getString(R.string.label_date).padEnd(pad)}: ${result.date}")
                    appendLine("${getString(R.string.label_time).padEnd(pad)}: ${result.time}")
                }
            }.trimEnd()
            // Show in popup — no auto-dismiss, stay open until OK
            progressDialog?.showResult(
                success = result.success,
                message = resultMsg,
                details = detailsForDialog,
                autoDismissMs = 0
            ) ?: run { progressDialog = null }

            binding.cardResult.visibility = View.VISIBLE
            binding.cardIntermediateStatus.visibility = View.GONE

            // Auto-fill receipt number for subsequent operations (reversal, book total, etc.)
            if (result.success && result.receiptNumber > 0) {
                binding.etReceiptNumber.setText(result.receiptNumber.toString())
            }

            if (result.success) {
                binding.tvResultIcon.text = "\u2713"
                binding.tvResultIcon.setTextColor(0xFF4CAF50.toInt())
                binding.tvResultTitle.text = getString(R.string.transaction_success)
                binding.tvResultTitle.setTextColor(0xFF4CAF50.toInt())
            } else {
                binding.tvResultIcon.text = "\u2717"
                binding.tvResultIcon.setTextColor(0xFFF44336.toInt())
                binding.tvResultTitle.text = getString(R.string.transaction_failed)
                binding.tvResultTitle.setTextColor(0xFFF44336.toInt())
            }
            binding.tvResultDetails.text = detailsForDialog
        }

        viewModel.receiptText.observe(viewLifecycleOwner) { text ->
            if (text.isNullOrEmpty()) {
                binding.cardReceipt.visibility = View.GONE
            } else {
                binding.cardReceipt.visibility = View.VISIBLE
                binding.tvReceipt.text = text
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error.isNullOrEmpty()) {
                binding.cardError.visibility = View.GONE
            } else {
                binding.cardError.visibility = View.VISIBLE
                binding.tvError.text = error
                // Show error in dialog if still visible
                progressDialog?.showResult(false, error)
                    ?: run { progressDialog = null }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
