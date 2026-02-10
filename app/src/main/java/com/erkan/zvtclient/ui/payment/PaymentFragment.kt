package com.erkan.zvtclient.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.erkan.zvtclient.databinding.FragmentPaymentBinding
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
            }
        }

        binding.btnRefund.setOnClickListener {
            val amount = binding.etAmount.text.toString()
            if (amount.isNotEmpty()) {
                viewModel.refund(amount)
            }
        }

        binding.btnReversal.setOnClickListener {
            viewModel.reversal()
        }

        binding.btnAbort.setOnClickListener {
            viewModel.abort()
        }
    }

    private fun observeViewModel() {
        viewModel.isProcessing.observe(viewLifecycleOwner) { processing ->
            binding.btnAuthorize.isEnabled = !processing
            binding.btnRefund.isEnabled = !processing
            binding.btnReversal.isEnabled = !processing
        }

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
                binding.tvResultTitle.text = "İşlem Başarılı"
                binding.tvResultTitle.setTextColor(0xFF4CAF50.toInt())
            } else {
                binding.tvResultIcon.text = "✗"
                binding.tvResultIcon.setTextColor(0xFFF44336.toInt())
                binding.tvResultTitle.text = "İşlem Başarısız"
                binding.tvResultTitle.setTextColor(0xFFF44336.toInt())
            }

            val details = buildString {
                appendLine("Tutar     : ${result.amountFormatted}")
                appendLine("Sonuç     : ${result.resultMessage}")
                if (result.traceNumber > 0) appendLine("Trace No  : ${result.traceNumber}")
                if (result.receiptNumber > 0) appendLine("Fiş No    : ${result.receiptNumber}")
                if (result.terminalId.isNotEmpty()) appendLine("Terminal  : ${result.terminalId}")
                result.cardData?.let { card ->
                    appendLine("─".repeat(30))
                    if (card.cardType.isNotEmpty()) appendLine("Kart Tipi : ${card.cardType}")
                    if (card.maskedPan.isNotEmpty()) appendLine("Kart No   : ${card.maskedPan}")
                    if (card.cardName.isNotEmpty()) appendLine("Kart Adı  : ${card.cardName}")
                }
                if (result.date.isNotEmpty()) {
                    appendLine("─".repeat(30))
                    appendLine("Tarih     : ${result.date}")
                    appendLine("Saat      : ${result.time}")
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
