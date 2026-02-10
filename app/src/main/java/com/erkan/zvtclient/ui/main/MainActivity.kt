package com.erkan.zvtclient.ui.main

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.erkan.zvt.model.ConnectionState
import com.erkan.zvtclient.R
import com.erkan.zvtclient.databinding.ActivityMainBinding
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
                binding.etHost.error = "IP adresi gerekli"
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
                binding.tvConnectionState.text = "Bağlı değil"
                binding.viewConnectionIndicator.setBackgroundResource(R.drawable.circle_red)
                binding.btnConnect.isEnabled = true
                binding.btnDisconnect.isEnabled = false
            }
            ConnectionState.CONNECTING, ConnectionState.REGISTERING -> {
                binding.tvConnectionState.text = "Bağlanıyor..."
                binding.btnConnect.isEnabled = false
                binding.btnDisconnect.isEnabled = true
            }
            ConnectionState.CONNECTED -> {
                binding.tvConnectionState.text = "Bağlı (kayıtsız)"
                binding.viewConnectionIndicator.setBackgroundResource(R.drawable.circle_green)
                binding.btnConnect.isEnabled = false
                binding.btnDisconnect.isEnabled = true
            }
            ConnectionState.REGISTERED -> {
                binding.tvConnectionState.text = "Bağlı ve Kayıtlı ✓"
                binding.viewConnectionIndicator.setBackgroundResource(R.drawable.circle_green)
                binding.btnConnect.isEnabled = false
                binding.btnDisconnect.isEnabled = true
            }
            ConnectionState.ERROR -> {
                binding.tvConnectionState.text = "Hata!"
                binding.viewConnectionIndicator.setBackgroundResource(R.drawable.circle_red)
                binding.btnConnect.isEnabled = true
                binding.btnDisconnect.isEnabled = true
            }
        }
    }
}
