package com.panda_erkan.zvtclientdemo.ui.common

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CompoundButton
import androidx.fragment.app.DialogFragment
import com.panda.zvt_library.protocol.ZvtConstants
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.databinding.DialogRegistrationConfigBinding

class RegistrationConfigDialog : DialogFragment() {

    private var _binding: DialogRegistrationConfigBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: SharedPreferences
    private var onSave: ((configByte: Byte, tlvEnabled: Boolean) -> Unit)? = null

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
        _binding = DialogRegistrationConfigBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadSettings()
        updateConfigByteDisplay()

        val checkChangeListener = CompoundButton.OnCheckedChangeListener { _, _ ->
            updateConfigByteDisplay()
        }
        binding.cbReceiptPayment.setOnCheckedChangeListener(checkChangeListener)
        binding.cbReceiptAdmin.setOnCheckedChangeListener(checkChangeListener)
        binding.cbIntermediateStatus.setOnCheckedChangeListener(checkChangeListener)
        binding.cbAllowPayment.setOnCheckedChangeListener(checkChangeListener)
        binding.cbAllowAdmin.setOnCheckedChangeListener(checkChangeListener)
        binding.cbTlvSupport.setOnCheckedChangeListener(checkChangeListener)

        binding.btnSaveConfig.setOnClickListener {
            saveSettings()
            onSave?.invoke(calculateConfigByte(), binding.cbTlvSupport.isChecked)
            dismissAllowingStateLoss()
        }

        // Dismiss on outside tap
        binding.root.setOnClickListener { dismissAllowingStateLoss() }
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

    private fun loadSettings() {
        binding.cbReceiptPayment.isChecked = prefs.getBoolean(KEY_RECEIPT_PAYMENT, false)
        binding.cbReceiptAdmin.isChecked = prefs.getBoolean(KEY_RECEIPT_ADMIN, false)
        binding.cbIntermediateStatus.isChecked = prefs.getBoolean(KEY_INTERMEDIATE_STATUS, true)
        binding.cbAllowPayment.isChecked = prefs.getBoolean(KEY_ALLOW_PAYMENT, false)
        binding.cbAllowAdmin.isChecked = prefs.getBoolean(KEY_ALLOW_ADMIN, false)
        binding.cbTlvSupport.isChecked = prefs.getBoolean(KEY_TLV_SUPPORT, true)
    }

    private fun saveSettings() {
        prefs.edit()
            .putBoolean(KEY_RECEIPT_PAYMENT, binding.cbReceiptPayment.isChecked)
            .putBoolean(KEY_RECEIPT_ADMIN, binding.cbReceiptAdmin.isChecked)
            .putBoolean(KEY_INTERMEDIATE_STATUS, binding.cbIntermediateStatus.isChecked)
            .putBoolean(KEY_ALLOW_PAYMENT, binding.cbAllowPayment.isChecked)
            .putBoolean(KEY_ALLOW_ADMIN, binding.cbAllowAdmin.isChecked)
            .putBoolean(KEY_TLV_SUPPORT, binding.cbTlvSupport.isChecked)
            .apply()
    }

    private fun calculateConfigByte(): Byte {
        var config = 0
        // Bit 1 (0x02): ECR prints payment receipt (checked = ECR prints, unchecked = PT prints)
        if (binding.cbReceiptPayment.isChecked) config = config or ZvtConstants.REG_ECR_PRINTS_PAYMENT_RECEIPT
        // Bit 2 (0x04): ECR prints admin receipt
        if (binding.cbReceiptAdmin.isChecked) config = config or ZvtConstants.REG_ECR_PRINTS_ADMIN_RECEIPT
        // Bit 3 (0x08): Send intermediate status
        if (binding.cbIntermediateStatus.isChecked) config = config or ZvtConstants.REG_INTERMEDIATE_STATUS.toInt()
        // Bit 4 (0x10): ECR controls payment
        if (binding.cbAllowPayment.isChecked) config = config or ZvtConstants.REG_ECR_CONTROLS_PAYMENT
        // Bit 5 (0x20): ECR controls admin
        if (binding.cbAllowAdmin.isChecked) config = config or ZvtConstants.REG_ECR_CONTROLS_ADMIN
        return config.toByte()
    }

    private fun updateConfigByteDisplay() {
        val configByte = calculateConfigByte()
        val value = configByte.toInt() and 0xFF
        binding.tvConfigByteValue.text = String.format("0x%02X (%d)", value, value)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "RegistrationConfigDialog"
        private const val PREFS_NAME = "zvt_reg_config"
        private const val KEY_RECEIPT_PAYMENT = "receipt_payment"
        private const val KEY_RECEIPT_ADMIN = "receipt_admin"
        private const val KEY_INTERMEDIATE_STATUS = "intermediate_status"
        private const val KEY_ALLOW_PAYMENT = "allow_payment"
        private const val KEY_ALLOW_ADMIN = "allow_admin"
        private const val KEY_TLV_SUPPORT = "tlv_support"

        fun newInstance(
            onSave: ((configByte: Byte, tlvEnabled: Boolean) -> Unit)? = null
        ): RegistrationConfigDialog {
            return RegistrationConfigDialog().apply {
                this.onSave = onSave
            }
        }

        fun getSavedConfigByte(context: Context): Byte {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var config = 0
            if (prefs.getBoolean(KEY_RECEIPT_PAYMENT, false))
                config = config or ZvtConstants.REG_ECR_PRINTS_PAYMENT_RECEIPT
            if (prefs.getBoolean(KEY_RECEIPT_ADMIN, false))
                config = config or ZvtConstants.REG_ECR_PRINTS_ADMIN_RECEIPT
            if (prefs.getBoolean(KEY_INTERMEDIATE_STATUS, true))
                config = config or ZvtConstants.REG_INTERMEDIATE_STATUS.toInt()
            if (prefs.getBoolean(KEY_ALLOW_PAYMENT, false))
                config = config or ZvtConstants.REG_ECR_CONTROLS_PAYMENT
            if (prefs.getBoolean(KEY_ALLOW_ADMIN, false))
                config = config or ZvtConstants.REG_ECR_CONTROLS_ADMIN
            return config.toByte()
        }

        fun isTlvEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(KEY_TLV_SUPPORT, true)
        }
    }
}
