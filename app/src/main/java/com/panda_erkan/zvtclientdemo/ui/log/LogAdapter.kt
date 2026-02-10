package com.panda_erkan.zvtclientdemo.ui.log

import android.graphics.drawable.GradientDrawable
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

            val info = resolveLogInfo(entry)

            // Icon badge
            binding.tvLogIcon.text = info.icon
            val iconBg = binding.tvLogIcon.background
            if (iconBg is GradientDrawable) {
                iconBg.setColor(info.iconColor)
            } else {
                val shape = GradientDrawable()
                shape.shape = GradientDrawable.OVAL
                shape.setColor(info.iconColor)
                binding.tvLogIcon.background = shape
            }

            // Message text color
            binding.tvLogMessage.setTextColor(info.textColor)

            // Level label
            binding.tvLogLevel.text = info.label
            binding.tvLogLevel.setTextColor(info.iconColor)

            // Subtle row background
            binding.logItemRoot.setBackgroundColor(info.rowBg)
        }

        private fun resolveLogInfo(entry: LogEntry): LogInfo {
            val msg = entry.message

            // Smart icon based on message content
            return when {
                // Completed / success messages
                msg.contains("\u2713") || msg.contains("successful") || msg.contains("basarili")
                        || msg.contains("erfolgreich") -> LogInfo(
                    icon = "\u2713",      // checkmark
                    iconColor = 0xFF2E7D32.toInt(),  // green 800
                    textColor = 0xFF1B5E20.toInt(),  // green 900
                    rowBg = 0x0C4CAF50.toInt(),      // green 5% alpha
                    label = "OK"
                )

                // Starting / in-progress messages
                msg.contains("\u25B6") || msg.contains("starting") || msg.contains("baslatiliyor")
                        || msg.contains("gestartet") -> LogInfo(
                    icon = "\u25B6",      // play triangle
                    iconColor = 0xFF00897B.toInt(),  // teal 600
                    textColor = 0xFF00695C.toInt(),  // teal 800
                    rowBg = 0x0C00897B.toInt(),      // teal 5% alpha
                    label = "RUN"
                )

                // Error messages
                entry.level == LogLevel.ERROR || msg.contains("\u2717") || msg.contains("error")
                        || msg.contains("hata") || msg.contains("Fehler") -> LogInfo(
                    icon = "\u2716",      // heavy X
                    iconColor = 0xFFD32F2F.toInt(),  // red 700
                    textColor = 0xFFC62828.toInt(),  // red 800
                    rowBg = 0x0CF44336.toInt(),      // red 5% alpha
                    label = "ERR"
                )

                // Warning messages
                entry.level == LogLevel.WARN -> LogInfo(
                    icon = "\u26A0",      // warning triangle
                    iconColor = 0xFFEF6C00.toInt(),  // orange 800
                    textColor = 0xFFE65100.toInt(),  // orange 900
                    rowBg = 0x0CFF9800.toInt(),      // orange 5% alpha
                    label = "WRN"
                )

                // Connection messages
                msg.startsWith("Connection") || msg.startsWith("Baglanti") || msg.startsWith("Verbindung") -> LogInfo(
                    icon = "\u21C5",      // up-down arrows
                    iconColor = 0xFF0097A7.toInt(),  // cyan 700
                    textColor = 0xFF006064.toInt(),  // cyan 900
                    rowBg = 0x0C0097A7.toInt(),      // cyan 5% alpha
                    label = "NET"
                )

                // Terminal / intermediate status
                msg.startsWith("Terminal:") -> LogInfo(
                    icon = "\u25A0",      // filled square
                    iconColor = 0xFF7B1FA2.toInt(),  // purple 700
                    textColor = 0xFF4A148C.toInt(),  // purple 900
                    rowBg = 0x0C9C27B0.toInt(),      // purple 5% alpha
                    label = "PT"
                )

                // Print / receipt lines
                msg.startsWith("Print:") || msg.startsWith("Fis:") || msg.startsWith("Druck:") -> LogInfo(
                    icon = "\u2399",      // print icon
                    iconColor = 0xFF5D4037.toInt(),  // brown 700
                    textColor = 0xFF3E2723.toInt(),  // brown 900
                    rowBg = 0x0C795548.toInt(),      // brown 5% alpha
                    label = "PRN"
                )

                // TX/RX protocol trace
                msg.contains("ECR -> PT") || msg.contains("TX ") -> LogInfo(
                    icon = "\u2191",      // up arrow
                    iconColor = 0xFF1565C0.toInt(),  // blue 800
                    textColor = 0xFF0D47A1.toInt(),  // blue 900
                    rowBg = 0x0C2196F3.toInt(),      // blue 5% alpha
                    label = "TX"
                )
                msg.contains("PT -> ECR") || msg.contains("RX ") -> LogInfo(
                    icon = "\u2193",      // down arrow
                    iconColor = 0xFF00838F.toInt(),  // cyan 800
                    textColor = 0xFF006064.toInt(),  // cyan 900
                    rowBg = 0x0C00BCD4.toInt(),      // cyan 5% alpha
                    label = "RX"
                )

                // Debug / default
                entry.level == LogLevel.DEBUG -> LogInfo(
                    icon = "\u2022",      // bullet
                    iconColor = 0xFF757575.toInt(),  // gray 600
                    textColor = 0xFF616161.toInt(),  // gray 700
                    rowBg = 0x00000000.toInt(),       // transparent
                    label = "DBG"
                )

                // Info fallback
                else -> LogInfo(
                    icon = "\u2139",      // info circle
                    iconColor = 0xFF00897B.toInt(),  // teal 600
                    textColor = 0xFF424242.toInt(),  // gray 800
                    rowBg = 0x0C00897B.toInt(),      // teal 5% alpha
                    label = "INF"
                )
            }
        }
    }

    private data class LogInfo(
        val icon: String,
        val iconColor: Int,
        val textColor: Int,
        val rowBg: Int,
        val label: String
    )

    class LogDiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry) =
            oldItem.timestamp == newItem.timestamp && oldItem.message == newItem.message

        override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry) =
            oldItem == newItem
    }
}
