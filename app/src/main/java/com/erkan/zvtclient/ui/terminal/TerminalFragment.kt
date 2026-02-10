package com.erkan.zvtclient.ui.terminal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.erkan.zvtclient.databinding.FragmentTerminalBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class TerminalFragment : Fragment() {

    private var _binding: FragmentTerminalBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TerminalViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTerminalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
        observeViewModel()
    }

    private fun setupButtons() {
        binding.btnDiagnosis.setOnClickListener {
            viewModel.diagnosis()
        }

        binding.btnStatusEnquiry.setOnClickListener {
            viewModel.statusEnquiry()
        }

        binding.btnEndOfDay.setOnClickListener {
            viewModel.endOfDay()
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressTerminal.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnDiagnosis.isEnabled = !loading
            binding.btnStatusEnquiry.isEnabled = !loading
            binding.btnEndOfDay.isEnabled = !loading
        }

        viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
            binding.tvTerminalStatus.text = message
        }

        viewModel.diagnosisResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            showResult(
                title = "Tanılama Sonucu",
                details = buildString {
                    appendLine("Durum    : ${if (result.success) "✓ Başarılı" else "✗ Başarısız"}")
                    appendLine("Bağlantı : ${if (result.status.isConnected) "Aktif" else "Kopuk"}")
                    if (result.status.terminalId.isNotEmpty())
                        appendLine("TID      : ${result.status.terminalId}")
                    if (result.errorMessage.isNotEmpty())
                        appendLine("Hata     : ${result.errorMessage}")
                }
            )
        }

        viewModel.endOfDayResult.observe(viewLifecycleOwner) { result ->
            result ?: return@observe
            showResult(
                title = "Gün Sonu Sonucu",
                details = buildString {
                    appendLine("Durum  : ${if (result.success) "✓ Başarılı" else "✗ Başarısız"}")
                    appendLine("Mesaj  : ${result.message}")
                    if (result.receiptLines.isNotEmpty()) {
                        appendLine("─".repeat(30))
                        result.receiptLines.forEach { appendLine(it) }
                    }
                }
            )
        }

        viewModel.terminalStatus.observe(viewLifecycleOwner) { status ->
            status ?: return@observe
            showResult(
                title = "Terminal Durumu",
                details = buildString {
                    appendLine("Bağlantı  : ${if (status.isConnected) "Aktif" else "Kopuk"}")
                    if (status.terminalId.isNotEmpty())
                        appendLine("TID       : ${status.terminalId}")
                    if (status.statusMessage.isNotEmpty())
                        appendLine("Mesaj     : ${status.statusMessage}")
                }
            )
        }
    }

    private fun showResult(title: String, details: String) {
        binding.cardTerminalResult.visibility = View.VISIBLE
        binding.tvResultTitle.text = title
        binding.tvResultDetails.text = details
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
