package com.panda_erkan.zvtclientdemo.ui.common

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.databinding.DialogProgressStatusBinding

class ProgressStatusDialog : DialogFragment() {

    private var _binding: DialogProgressStatusBinding? = null
    private val binding get() = _binding!!

    private var operationName: String = ""
    private var operationIcon: String = ""
    private var onCancelClick: (() -> Unit)? = null
    private var cancelSent = false
    var resultShown = false
        private set

    private val handler = Handler(Looper.getMainLooper())
    private var startTimeMs = 0L
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (_binding == null) return
            val elapsed = System.currentTimeMillis() - startTimeMs
            val seconds = (elapsed / 1000) % 60
            val minutes = (elapsed / 1000) / 60
            binding.tvElapsedTime.text = String.format("%02d:%02d", minutes, seconds)
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.ProgressDialogTheme)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogProgressStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvDialogOperationName.text = operationName
        binding.tvDialogOperationIcon.text = operationIcon

        binding.btnDialogCancel.setOnClickListener {
            if (!cancelSent) {
                cancelSent = true
                // Send abort command to terminal
                onCancelClick?.invoke()
                // Update UI to show abort was sent - keep dialog open
                binding.btnDialogCancel.isEnabled = false
                binding.btnDialogCancel.text = getString(R.string.abort_sent)
                binding.btnDialogCancel.setTextColor(resources.getColor(R.color.outline, null))
                binding.btnDialogCancel.iconTint =
                    android.content.res.ColorStateList.valueOf(resources.getColor(R.color.outline, null))
                // Show status
                updateStatus(getString(R.string.abort_waiting_terminal))
            }
        }

        // Close (X) button in top-right corner — always dismisses the dialog
        binding.btnCloseDialog.setOnClickListener {
            dismissAllowingStateLoss()
        }

        // Start elapsed timer
        startTimeMs = System.currentTimeMillis()
        handler.post(timerRunnable)
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

    fun updateStatus(message: String) {
        if (_binding == null) return
        if (message.isEmpty()) {
            binding.cardStatusMessage.visibility = View.GONE
        } else {
            binding.cardStatusMessage.visibility = View.VISIBLE
            binding.tvStatusMessage.text = message

            binding.tvStatusIcon.text = when {
                message.contains("PIN", ignoreCase = true) -> "\uD83D\uDD10"
                message.contains("card", ignoreCase = true)
                        || message.contains("Kart", ignoreCase = true)
                        || message.contains("Karte", ignoreCase = true) -> "\uD83D\uDCB3"
                message.contains("wait", ignoreCase = true)
                        || message.contains("bekle", ignoreCase = true)
                        || message.contains("warten", ignoreCase = true) -> "\u23F3"
                message.contains("abort", ignoreCase = true)
                        || message.contains("iptal", ignoreCase = true)
                        || message.contains("Abbruch", ignoreCase = true) -> "\u26D4"
                message.contains("approved", ignoreCase = true)
                        || message.contains("onaylandi", ignoreCase = true)
                        || message.contains("genehmigt", ignoreCase = true) -> "\u2705"
                message.contains("declined", ignoreCase = true)
                        || message.contains("reddedildi", ignoreCase = true)
                        || message.contains("abgelehnt", ignoreCase = true) -> "\u274C"
                message.contains("timeout", ignoreCase = true)
                        || message.contains("zaman", ignoreCase = true)
                        || message.contains("Zeitüberschreitung", ignoreCase = true) -> "\u23F0"
                else -> "\uD83D\uDCAC"
            }
        }
    }

    /**
     * Show the final result in the dialog, then auto-dismiss after delay.
     * @param success true = green success, false = red error/abort
     * @param message result message to display
     * @param autoDismissMs delay before auto-dismiss (0 = no auto-dismiss)
     */
    fun showResult(success: Boolean, message: String, autoDismissMs: Long = 4000) {
        if (_binding == null) return
        if (resultShown) return
        resultShown = true

        // Stop timer
        handler.removeCallbacks(timerRunnable)

        // Hide progress indicator
        binding.progressIndicator.visibility = View.GONE
        binding.tvWaitingMessage.visibility = View.GONE

        // Show result card
        binding.cardResult.visibility = View.VISIBLE
        if (success) {
            binding.tvResultIcon.text = "\u2705"
            binding.tvResultMessage.text = message
            binding.tvResultMessage.setTextColor(resources.getColor(R.color.success, null))
            binding.cardResult.setCardBackgroundColor(resources.getColor(R.color.successContainer, null))
            binding.cardResult.strokeColor = resources.getColor(R.color.success, null)
        } else {
            // Differentiate abort vs timeout vs generic error
            val isTimeout = message.contains("timeout", ignoreCase = true)
                    || message.contains("zaman", ignoreCase = true)
                    || message.contains("Zeitüberschreitung", ignoreCase = true)
            val isAbort = message.contains("abort", ignoreCase = true)
                    || message.contains("iptal", ignoreCase = true)
                    || message.contains("Abbruch", ignoreCase = true)

            binding.tvResultIcon.text = when {
                isTimeout -> "\u23F0"    // alarm clock
                isAbort -> "\u26D4"      // no entry
                else -> "\u274C"         // X mark
            }
            binding.tvResultMessage.text = message
            binding.tvResultMessage.setTextColor(resources.getColor(R.color.error, null))
            binding.cardResult.setCardBackgroundColor(resources.getColor(R.color.errorContainer, null))
            binding.cardResult.strokeColor = resources.getColor(R.color.error, null)
        }

        // Update header
        val headerBg = if (success) resources.getColor(R.color.success, null)
        else resources.getColor(R.color.error, null)
        binding.tvDialogOperationName.parent.let { parent ->
            (parent as? View)?.setBackgroundColor(headerBg)
        }

        // Change cancel button to close button
        binding.btnDialogCancel.visibility = View.VISIBLE
        binding.btnDialogCancel.isEnabled = true
        binding.btnDialogCancel.text = "\u2716"
        binding.btnDialogCancel.setTextColor(resources.getColor(R.color.onSurfaceVariant, null))
        binding.btnDialogCancel.iconTint = null
        binding.btnDialogCancel.icon = null
        binding.btnDialogCancel.setOnClickListener { dismissAllowingStateLoss() }

        // Auto-dismiss after delay
        if (autoDismissMs > 0) {
            handler.postDelayed({
                dismissAllowingStateLoss()
            }, autoDismissMs)
        }
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "ProgressStatusDialog"

        fun newInstance(
            operationName: String,
            operationIcon: String = "\uD83D\uDD04",
            onCancel: (() -> Unit)? = null
        ): ProgressStatusDialog {
            return ProgressStatusDialog().apply {
                this.operationName = operationName
                this.operationIcon = operationIcon
                this.onCancelClick = onCancel
            }
        }
    }
}
