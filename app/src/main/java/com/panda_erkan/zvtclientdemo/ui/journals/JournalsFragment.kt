package com.panda_erkan.zvtclientdemo.ui.journals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.data.model.OperationType
import com.panda_erkan.zvtclientdemo.databinding.FragmentJournalsBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class JournalsFragment : Fragment() {

    private var _binding: FragmentJournalsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: JournalsViewModel by viewModel()
    private lateinit var adapter: JournalAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJournalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupChipFilters()
        setupClearButton()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = JournalAdapter { entry ->
            viewModel.selectEntry(entry)
        }
        binding.rvJournals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvJournals.adapter = adapter
    }

    private fun setupChipFilters() {
        binding.chipAll.setOnClickListener { viewModel.setFilter(null) }
        binding.chipPayments.setOnClickListener { viewModel.setFilter(OperationType.PAYMENT) }
        binding.chipRefunds.setOnClickListener { viewModel.setFilter(OperationType.REFUND) }
        binding.chipReversals.setOnClickListener { viewModel.setFilter(OperationType.REVERSAL) }
        binding.chipPreAuth.setOnClickListener { viewModel.setFilter(OperationType.PRE_AUTHORIZATION) }
        binding.chipEndOfDay.setOnClickListener { viewModel.setFilter(OperationType.END_OF_DAY) }
        binding.chipDiagnosis.setOnClickListener { viewModel.setFilter(OperationType.DIAGNOSIS) }
    }

    private fun setupClearButton() {
        binding.btnClearJournals.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.journal_clear_confirm))
                .setPositiveButton(getString(R.string.btn_clear)) { _, _ ->
                    viewModel.clearAllEntries()
                }
                .setNegativeButton(getString(R.string.btn_close), null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewModel.entries.observe(viewLifecycleOwner) { entries ->
            adapter.submitList(entries)
            binding.tvEmptyState.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
            binding.rvJournals.visibility = if (entries.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.selectedEntry.observe(viewLifecycleOwner) { entry ->
            if (entry != null) {
                JournalDetailDialog.newInstance(entry)
                    .show(childFragmentManager, JournalDetailDialog.TAG)
                viewModel.clearSelectedEntry()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
