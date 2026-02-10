package com.panda_erkan.zvtclientdemo.ui.log

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.panda_erkan.zvtclientdemo.databinding.FragmentLogBinding
import com.panda_erkan.zvtclientdemo.ui.main.MainViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModel()
    private val logAdapter = LogAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvLogs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = logAdapter
        }

        binding.btnClearLogs.setOnClickListener {
            viewModel.clearLogs()
        }

        viewModel.logEntries.observe(viewLifecycleOwner) { entries ->
            logAdapter.submitList(entries) {
                // Otomatik en alta kaydır
                if (entries.isNotEmpty()) {
                    binding.rvLogs.scrollToPosition(entries.size - 1)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
