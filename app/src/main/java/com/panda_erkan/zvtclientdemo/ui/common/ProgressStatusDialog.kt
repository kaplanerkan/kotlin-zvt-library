package com.panda_erkan.zvtclientdemo.ui.common

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
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

    private val timerHandler = Handler(Looper.getMainLooper())
    private var startTimeMs = 0L
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (_binding == null) return
            val elapsed = System.currentTimeMillis() - startTimeMs
            val seconds = (elapsed / 1000) % 60
            val minutes = (elapsed / 1000) / 60
            binding.tvElapsedTime.text = String.format("%02d:%02d", minutes, seconds)
            timerHandler.postDelayed(this, 1000)
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
            onCancelClick?.invoke()
        }

        // Start elapsed timer
        startTimeMs = System.currentTimeMillis()
        timerHandler.post(timerRunnable)
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

            // Change icon based on status content
            binding.tvStatusIcon.text = when {
                message.contains("PIN", ignoreCase = true) -> "\uD83D\uDD10"     // lock
                message.contains("card", ignoreCase = true)
                        || message.contains("Kart", ignoreCase = true)
                        || message.contains("Karte", ignoreCase = true) -> "\uD83D\uDCB3" // card
                message.contains("wait", ignoreCase = true)
                        || message.contains("bekle", ignoreCase = true)
                        || message.contains("warten", ignoreCase = true) -> "\u23F3"      // hourglass
                message.contains("approved", ignoreCase = true)
                        || message.contains("onaylandi", ignoreCase = true)
                        || message.contains("genehmigt", ignoreCase = true) -> "\u2705"   // check
                message.contains("declined", ignoreCase = true)
                        || message.contains("reddedildi", ignoreCase = true)
                        || message.contains("abgelehnt", ignoreCase = true) -> "\u274C"   // X
                else -> "\uD83D\uDCAC"  // speech bubble
            }
        }
    }

    override fun onDestroyView() {
        timerHandler.removeCallbacks(timerRunnable)
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
