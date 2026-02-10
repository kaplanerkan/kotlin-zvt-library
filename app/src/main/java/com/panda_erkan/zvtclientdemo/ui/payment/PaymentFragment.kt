package com.panda_erkan.zvtclientdemo.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.panda.zvt_library.model.ConnectionState
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.databinding.FragmentPaymentBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class PaymentFragment : Fragment() {

    private var _binding: FragmentPaymentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PaymentViewModel by viewModel()

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
                viewModel.authorize(amount)
            } else {
                binding.etAmount.error = getString(R.string.error_amount_required)
            }
        }

        binding.btnRefund.setOnClickListener {
            val amount = binding.etAmount.text.toString()
            if (amount.isNotEmpty()) {
                viewModel.refund(amount)
            } else {
                binding.etAmount.error = getString(R.string.error_amount_required)
            }
        }

        binding.btnReversal.setOnClickListener {
            viewModel.reversal()
        }

        binding.btnAbort.setOnClickListener {
            viewModel.abort()
        }

        binding.btnPreAuth.setOnClickListener {
            val amount = binding.etAmount.text.toString()
            if (amount.isNotEmpty()) {
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
                viewModel.partialReversal(amount, receiptNo)
            }
        }
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
        viewModel.isProcessing.observe(viewLifecycleOwner) { updateButtonStates() }

        viewModel.intermediateStatus.observe(viewLifecycleOwner) { status ->
            if (status.isNullOrEmpty()) {
                binding.cardIntermediateStatus.visibility = View.GONE
            } else {
                binding.cardIntermediateStatus.visibility = View.VISIBLE
                binding.tvIntermediateStatus.text = status
            }
        }

        viewModel.transactionResult.observe(viewLifecycleOwner) { result ->
            if (result == null) {
                binding.cardResult.visibility = View.GONE
                return@observe
            }

            binding.cardResult.visibility = View.VISIBLE
            binding.cardIntermediateStatus.visibility = View.GONE

            if (result.success) {
                binding.tvResultIcon.text = "✓"
                binding.tvResultIcon.setTextColor(0xFF4CAF50.toInt())
                binding.tvResultTitle.text = getString(R.string.transaction_success)
                binding.tvResultTitle.setTextColor(0xFF4CAF50.toInt())
            } else {
                binding.tvResultIcon.text = "✗"
                binding.tvResultIcon.setTextColor(0xFFF44336.toInt())
                binding.tvResultTitle.text = getString(R.string.transaction_failed)
                binding.tvResultTitle.setTextColor(0xFFF44336.toInt())
            }

            val pad = 10
            val details = buildString {
                appendLine("${getString(R.string.label_amount).padEnd(pad)}: ${result.amountFormatted}")
                appendLine("${getString(R.string.label_result).padEnd(pad)}: ${result.resultMessage}")
                if (result.traceNumber > 0) appendLine("${getString(R.string.label_trace_no).padEnd(pad)}: ${result.traceNumber}")
                if (result.receiptNumber > 0) appendLine("${getString(R.string.label_receipt_no).padEnd(pad)}: ${result.receiptNumber}")
                if (result.terminalId.isNotEmpty()) appendLine("${getString(R.string.label_terminal).padEnd(pad)}: ${result.terminalId}")
                result.cardData?.let { card ->
                    appendLine("─".repeat(30))
                    if (card.cardType.isNotEmpty()) appendLine("${getString(R.string.label_card_type).padEnd(pad)}: ${card.cardType}")
                    if (card.maskedPan.isNotEmpty()) appendLine("${getString(R.string.label_card_no).padEnd(pad)}: ${card.maskedPan}")
                    if (card.cardName.isNotEmpty()) appendLine("${getString(R.string.label_card_name).padEnd(pad)}: ${card.cardName}")
                }
                if (result.date.isNotEmpty()) {
                    appendLine("─".repeat(30))
                    appendLine("${getString(R.string.label_date).padEnd(pad)}: ${result.date}")
                    appendLine("${getString(R.string.label_time).padEnd(pad)}: ${result.time}")
                }
            }
            binding.tvResultDetails.text = details
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
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
