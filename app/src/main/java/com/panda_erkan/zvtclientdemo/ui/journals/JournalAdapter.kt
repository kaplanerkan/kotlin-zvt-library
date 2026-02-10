package com.panda_erkan.zvtclientdemo.ui.journals

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.data.entity.JournalEntry
import com.panda_erkan.zvtclientdemo.data.model.OperationType
import com.panda_erkan.zvtclientdemo.databinding.ItemJournalBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JournalAdapter(
    private val onClick: (JournalEntry) -> Unit
) : ListAdapter<JournalEntry, JournalAdapter.JournalViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val binding = ItemJournalBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return JournalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class JournalViewHolder(
        private val binding: ItemJournalBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: JournalEntry) {
            val ctx = binding.root.context

            // Status icon
            binding.tvStatusIcon.text = if (entry.success) "\u2705" else "\u274C"

            // Operation type label
            binding.tvOperationType.text = entry.operationType.toDisplayName(ctx)

            // Amount
            val amountCents = entry.amountInCents
            if (amountCents != null && amountCents > 0) {
                binding.tvAmount.text = "%.2f EUR".format(amountCents / 100.0)
            } else {
                binding.tvAmount.text = ""
            }

            // Result message
            binding.tvResultMessage.text = entry.resultMessage

            // Card info
            val cardInfo = buildString {
                if (!entry.cardName.isNullOrEmpty()) append(entry.cardName)
                if (!entry.maskedPan.isNullOrEmpty()) {
                    if (isNotEmpty()) append(" \u2022 ")
                    append(entry.maskedPan)
                }
            }
            binding.tvCardInfo.text = cardInfo

            // Timestamp
            val sdf = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
            binding.tvTimestamp.text = sdf.format(Date(entry.timestamp))

            // Left indicator color
            val indicatorColor = entry.operationType.toColor(ctx)
            binding.viewTypeIndicator.setBackgroundColor(indicatorColor)

            // Click
            binding.root.setOnClickListener { onClick(entry) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<JournalEntry>() {
        override fun areItemsTheSame(oldItem: JournalEntry, newItem: JournalEntry) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: JournalEntry, newItem: JournalEntry) =
            oldItem == newItem
    }
}

fun OperationType.toDisplayName(ctx: android.content.Context): String = when (this) {
    OperationType.PAYMENT -> ctx.getString(R.string.op_payment)
    OperationType.REFUND -> ctx.getString(R.string.op_refund)
    OperationType.REVERSAL -> ctx.getString(R.string.op_reversal)
    OperationType.PRE_AUTHORIZATION -> ctx.getString(R.string.op_pre_auth)
    OperationType.BOOK_TOTAL -> ctx.getString(R.string.op_book_total)
    OperationType.PARTIAL_REVERSAL -> ctx.getString(R.string.op_partial_reversal)
    OperationType.END_OF_DAY -> ctx.getString(R.string.op_end_of_day)
    OperationType.DIAGNOSIS -> ctx.getString(R.string.op_diagnosis)
    OperationType.STATUS_ENQUIRY -> ctx.getString(R.string.op_status_enquiry)
    OperationType.REPEAT_RECEIPT -> ctx.getString(R.string.op_repeat_receipt)
    OperationType.LOG_OFF -> ctx.getString(R.string.op_log_off)
}

fun OperationType.toColor(ctx: android.content.Context): Int = when (this) {
    OperationType.PAYMENT -> ctx.getColor(R.color.btnPayment)
    OperationType.REFUND -> ctx.getColor(R.color.btnRefund)
    OperationType.REVERSAL -> ctx.getColor(R.color.btnReversal)
    OperationType.PRE_AUTHORIZATION -> ctx.getColor(R.color.btnPreAuth)
    OperationType.BOOK_TOTAL -> ctx.getColor(R.color.btnBookTotal)
    OperationType.PARTIAL_REVERSAL -> ctx.getColor(R.color.btnPartialRev)
    OperationType.END_OF_DAY -> ctx.getColor(R.color.btnEndOfDay)
    OperationType.DIAGNOSIS -> ctx.getColor(R.color.btnDiagnosis)
    OperationType.STATUS_ENQUIRY -> ctx.getColor(R.color.btnStatus)
    OperationType.REPEAT_RECEIPT -> ctx.getColor(R.color.btnRepeatReceipt)
    OperationType.LOG_OFF -> ctx.getColor(R.color.btnLogOff)
}

fun OperationType.toIcon(): String = when (this) {
    OperationType.PAYMENT -> "\uD83D\uDCB3"
    OperationType.REFUND -> "\uD83D\uDD04"
    OperationType.REVERSAL -> "\u21A9\uFE0F"
    OperationType.PRE_AUTHORIZATION -> "\uD83D\uDD12"
    OperationType.BOOK_TOTAL -> "\u2705"
    OperationType.PARTIAL_REVERSAL -> "\u2702\uFE0F"
    OperationType.END_OF_DAY -> "\uD83D\uDCC5"
    OperationType.DIAGNOSIS -> "\uD83D\uDCCA"
    OperationType.STATUS_ENQUIRY -> "\uD83D\uDD0D"
    OperationType.REPEAT_RECEIPT -> "\uD83D\uDDA8\uFE0F"
    OperationType.LOG_OFF -> "\uD83D\uDEAA"
}
