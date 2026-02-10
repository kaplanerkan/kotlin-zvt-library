package com.panda_erkan.zvtclientdemo.ui.journals

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.data.entity.JournalEntry
import com.panda_erkan.zvtclientdemo.databinding.DialogJournalDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JournalDetailDialog : DialogFragment() {

    private var _binding: DialogJournalDetailBinding? = null
    private val binding get() = _binding!!

    private var entry: JournalEntry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.ProgressDialogTheme)
        isCancelable = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogJournalDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val e = entry ?: run { dismissAllowingStateLoss(); return }

        // Header
        binding.tvOperationIcon.text = e.operationType.toIcon()
        binding.tvOperationName.text = e.operationType.toDisplayName(requireContext())
        val headerColor = e.operationType.toColor(requireContext())
        binding.headerBar.setBackgroundColor(headerColor)

        // Result card
        if (e.success) {
            binding.tvResultIcon.text = "\u2705"
            binding.tvResultMessage.text = getString(R.string.status_success)
            binding.tvResultMessage.setTextColor(resources.getColor(R.color.success, null))
            binding.cardResult.setCardBackgroundColor(resources.getColor(R.color.successContainer, null))
            binding.cardResult.strokeColor = resources.getColor(R.color.success, null)
        } else {
            binding.tvResultIcon.text = "\u274C"
            binding.tvResultMessage.text = e.resultMessage.ifEmpty { getString(R.string.status_failed) }
            binding.tvResultMessage.setTextColor(resources.getColor(R.color.error, null))
            binding.cardResult.setCardBackgroundColor(resources.getColor(R.color.errorContainer, null))
            binding.cardResult.strokeColor = resources.getColor(R.color.error, null)
        }

        // Details
        binding.tvResultDetails.text = buildDetailsText(e)

        // Buttons
        binding.btnOk.setOnClickListener { dismissAllowingStateLoss() }
        binding.btnCloseDialog.setOnClickListener { dismissAllowingStateLoss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            setBackgroundDrawable(ColorDrawable(Color.parseColor("#80000000")))
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    private fun buildDetailsText(e: JournalEntry): String {
        val pad = 12
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

        return buildString {
            // Timestamp
            appendLine("${getString(R.string.label_date).padEnd(pad)}: ${sdf.format(Date(e.timestamp))}")

            // Result
            appendLine("${getString(R.string.label_result).padEnd(pad)}: ${e.resultMessage}")

            // Amount
            if (e.amountInCents != null && e.amountInCents > 0) {
                appendLine("${getString(R.string.label_amount).padEnd(pad)}: ${"%.2f EUR".format(e.amountInCents / 100.0)}")
            }

            // Transaction fields
            if (e.traceNumber != null && e.traceNumber > 0)
                appendLine("${getString(R.string.label_trace_no).padEnd(pad)}: ${e.traceNumber}")
            if (e.receiptNumber != null && e.receiptNumber > 0)
                appendLine("${getString(R.string.label_receipt_no).padEnd(pad)}: ${e.receiptNumber}")
            if (!e.terminalId.isNullOrEmpty())
                appendLine("${getString(R.string.label_terminal).padEnd(pad)}: ${e.terminalId}")
            if (!e.vuNumber.isNullOrEmpty())
                appendLine("${getString(R.string.label_vu_number).padEnd(pad)}: ${e.vuNumber}")

            // Card data
            if (!e.cardType.isNullOrEmpty() || !e.maskedPan.isNullOrEmpty()) {
                appendLine("\u2500".repeat(30))
                if (!e.cardType.isNullOrEmpty())
                    appendLine("${getString(R.string.label_card_type).padEnd(pad)}: ${e.cardType}")
                if (!e.maskedPan.isNullOrEmpty())
                    appendLine("${getString(R.string.label_card_no).padEnd(pad)}: ${e.maskedPan}")
                if (!e.cardName.isNullOrEmpty())
                    appendLine("${getString(R.string.label_card_name).padEnd(pad)}: ${e.cardName}")
                if (!e.expiryDate.isNullOrEmpty())
                    appendLine("${getString(R.string.label_expiry).padEnd(pad)}: ${e.expiryDate}")
                if (e.cardSequenceNumber != null && e.cardSequenceNumber > 0)
                    appendLine("${getString(R.string.label_seq_no).padEnd(pad)}: ${e.cardSequenceNumber}")
                if (!e.aid.isNullOrEmpty())
                    appendLine("${getString(R.string.label_aid).padEnd(pad)}: ${e.aid}")
            }

            // Transaction date/time
            if (!e.transactionDate.isNullOrEmpty()) {
                appendLine("\u2500".repeat(30))
                appendLine("${getString(R.string.label_date).padEnd(pad)}: ${e.transactionDate}")
                if (!e.transactionTime.isNullOrEmpty())
                    appendLine("${getString(R.string.label_time).padEnd(pad)}: ${e.transactionTime}")
            }

            // End of Day specific
            if (e.transactionCount != null) {
                appendLine("\u2500".repeat(30))
                appendLine("Tx Count".padEnd(pad) + ": ${e.transactionCount}")
                if (e.totalAmountInCents != null && e.totalAmountInCents > 0)
                    appendLine("Total".padEnd(pad) + ": ${"%.2f EUR".format(e.totalAmountInCents / 100.0)}")
            }

            // Receipt lines
            if (!e.receiptLines.isNullOrEmpty()) {
                appendLine("\u2500".repeat(30))
                e.receiptLines.split("|||").forEach { appendLine(it) }
            }

            // Status message
            if (!e.statusMessage.isNullOrEmpty()) {
                appendLine("\u2500".repeat(30))
                appendLine("${getString(R.string.label_message).padEnd(pad)}: ${e.statusMessage}")
            }
        }.trimEnd()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "JournalDetailDialog"

        fun newInstance(entry: JournalEntry): JournalDetailDialog {
            return JournalDetailDialog().apply {
                this.entry = entry
            }
        }
    }
}
