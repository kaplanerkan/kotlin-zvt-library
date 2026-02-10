package com.panda_erkan.zvtclientdemo.ui.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.panda.zvt_library.model.ConnectionState
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.databinding.ActivityMainBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupConnectionUI()
        observeViewModel()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)
    }

    private fun setupConnectionUI() {
        binding.btnConnect.setOnClickListener {
            val host = binding.etHost.text.toString().trim()
            val port = binding.etPort.text.toString().trim().toIntOrNull() ?: 20007

            if (host.isEmpty()) {
                binding.etHost.error = getString(R.string.error_ip_required)
                return@setOnClickListener
            }

            viewModel.connectAndRegister(host, port)
        }

        binding.btnDisconnect.setOnClickListener {
            viewModel.disconnect()
        }
    }

    private fun observeViewModel() {
        viewModel.connectionState.observe(this) { state ->
            updateConnectionUI(state)
        }

        viewModel.statusMessage.observe(this) { message ->
            binding.tvStatusMessage.text = message
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressConnection.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnConnect.isEnabled = !loading
        }
    }

    private fun updateConnectionUI(state: ConnectionState) {
        when (state) {
            ConnectionState.DISCONNECTED -> {
                binding.tvConnectionState.text = getString(R.string.disconnected)
                binding.viewConnectionIndicator.setBackgroundResource(R.drawable.circle_red)
                binding.btnConnect.isEnabled = true
                binding.btnDisconnect.isEnabled = false
            }
            ConnectionState.CONNECTING, ConnectionState.REGISTERING -> {
                binding.tvConnectionState.text = getString(R.string.connecting)
                binding.btnConnect.isEnabled = false
                binding.btnDisconnect.isEnabled = true
            }
            ConnectionState.CONNECTED -> {
                binding.tvConnectionState.text = getString(R.string.connected_unregistered)
                binding.viewConnectionIndicator.setBackgroundResource(R.drawable.circle_green)
                binding.btnConnect.isEnabled = false
                binding.btnDisconnect.isEnabled = true
            }
            ConnectionState.REGISTERED -> {
                binding.tvConnectionState.text = getString(R.string.connected_registered)
                binding.viewConnectionIndicator.setBackgroundResource(R.drawable.circle_green)
                binding.btnConnect.isEnabled = false
                binding.btnDisconnect.isEnabled = true
            }
            ConnectionState.ERROR -> {
                binding.tvConnectionState.text = getString(R.string.connection_error)
                binding.viewConnectionIndicator.setBackgroundResource(R.drawable.circle_red)
                binding.btnConnect.isEnabled = true
                binding.btnDisconnect.isEnabled = true
            }
        }
    }
}
