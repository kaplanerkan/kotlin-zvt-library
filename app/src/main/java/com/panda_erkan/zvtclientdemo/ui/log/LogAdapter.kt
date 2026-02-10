package com.panda_erkan.zvtclientdemo.ui.log

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.panda_erkan.zvtclientdemo.databinding.ItemLogBinding
import com.panda_erkan.zvtclientdemo.repository.LogEntry
import com.panda_erkan.zvtclientdemo.repository.LogLevel

class LogAdapter : ListAdapter<LogEntry, LogAdapter.LogViewHolder>(LogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(
        private val binding: ItemLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: LogEntry) {
            binding.tvLogTime.text = entry.timeFormatted
            binding.tvLogMessage.text = entry.message

            val color = when (entry.level) {
                LogLevel.DEBUG -> 0xFF9E9E9E.toInt()
                LogLevel.INFO -> 0xFF4CAF50.toInt()
                LogLevel.WARN -> 0xFFFF9800.toInt()
                LogLevel.ERROR -> 0xFFF44336.toInt()
            }
            binding.viewLogLevel.setBackgroundColor(color)

            val textColor = when (entry.level) {
                LogLevel.ERROR -> 0xFFC62828.toInt()
                LogLevel.WARN -> 0xFFE65100.toInt()
                else -> 0xFF424242.toInt()
            }
            binding.tvLogMessage.setTextColor(textColor)
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry) =
            oldItem.timestamp == newItem.timestamp && oldItem.message == newItem.message

        override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry) =
            oldItem == newItem
    }
}
