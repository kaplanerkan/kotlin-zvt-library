package com.panda_erkan.zvtclientdemo.ui.main

import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.net.NetworkInterface
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.panda.zvt_library.model.ConnectionState
import com.panda_erkan.zvtclientdemo.R
import com.panda_erkan.zvtclientdemo.databinding.ActivityMainBinding
import com.panda_erkan.zvtclientdemo.ui.common.RegistrationConfigDialog
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModel()
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences("zvt_settings", MODE_PRIVATE)

        setupNavigation()
        setupConnectionUI()
        setupSimulatorToggle()
        setupLanguageSwitcher()
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

            val configByte = RegistrationConfigDialog.getSavedConfigByte(this)
            val tlvEnabled = RegistrationConfigDialog.isTlvEnabled(this)
            val keepAlive = binding.cbKeepAlive.isChecked
            viewModel.connectAndRegister(host, port, configByte, tlvEnabled, keepAlive)
        }

        binding.btnDisconnect.setOnClickListener {
            viewModel.disconnect()
        }

        binding.btnRegConfig.setOnClickListener {
            RegistrationConfigDialog.newInstance().show(supportFragmentManager, RegistrationConfigDialog.TAG)
        }
    }

    private fun setupSimulatorToggle() {
        binding.switchSimulator.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("simulator_mode", isChecked).apply()

            if (isChecked) {
                // Save current real terminal values
                prefs.edit()
                    .putString("real_terminal_host", binding.etHost.text.toString())
                    .putInt("real_terminal_port", binding.etPort.text.toString().toIntOrNull() ?: 20007)
                    .apply()
                // Start embedded simulator
                viewModel.startSimulator()
            } else {
                // Stop embedded simulator
                viewModel.stopSimulator()
                // Restore real terminal values
                binding.etHost.setText(prefs.getString("real_terminal_host", "192.168.1.135"))
                binding.etPort.setText(prefs.getInt("real_terminal_port", 20007).toString())
                binding.tvSimulatorHint.visibility = View.GONE
            }
        }

        // Observe simulator state
        viewModel.simulatorRunning.observe(this) { running ->
            if (running) {
                // Server started — set localhost and show hint
                binding.etHost.setText("127.0.0.1")
                binding.etPort.setText("20007")
                showDeviceIpHint()
            }
        }

        viewModel.simulatorStarting.observe(this) { starting ->
            binding.progressConnection.visibility = if (starting) View.VISIBLE else View.GONE
            binding.switchSimulator.isEnabled = !starting
            binding.btnConnect.isEnabled = !starting
        }
    }

    private fun showDeviceIpHint() {
        val ips = getDeviceIpAddresses()
        val deviceIp = ips.firstOrNull() ?: "N/A"
        val lines = mutableListOf<String>()
        lines.add("ZVT: 127.0.0.1:20007")
        lines.add(getString(R.string.simulator_api_url, deviceIp))
        binding.tvSimulatorHint.text = lines.joinToString("\n")
        binding.tvSimulatorHint.visibility = View.VISIBLE
    }

    private fun getDeviceIpAddresses(): List<String> {
        val ips = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return ips
            for (ni in interfaces) {
                if (ni.isLoopback || !ni.isUp) continue
                for (addr in ni.inetAddresses) {
                    if (addr.isLoopbackAddress) continue
                    val hostAddr = addr.hostAddress ?: continue
                    // IPv4 only
                    if (hostAddr.contains('.') && !hostAddr.contains(':')) {
                        ips.add(hostAddr)
                    }
                }
            }
        } catch (_: Exception) { }
        return ips
    }

    private fun setupLanguageSwitcher() {
        highlightActiveLanguage()

        binding.btnLangEn.setOnClickListener { setAppLocale("en") }
        binding.btnLangTr.setOnClickListener { setAppLocale("tr") }
        binding.btnLangDe.setOnClickListener { setAppLocale("de") }
    }

    private fun setAppLocale(languageTag: String) {
        val localeList = LocaleListCompat.forLanguageTags(languageTag)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    private fun highlightActiveLanguage() {
        val currentLocale = AppCompatDelegate.getApplicationLocales()
        val lang = if (!currentLocale.isEmpty) {
            currentLocale.get(0)?.language ?: "en"
        } else {
            resources.configuration.locales.get(0)?.language ?: "en"
        }

        val buttons = mapOf(
            "en" to binding.btnLangEn,
            "tr" to binding.btnLangTr,
            "de" to binding.btnLangDe
        )

        buttons.forEach { (code, btn) ->
            if (code == lang) {
                btn.setTypeface(null, Typeface.BOLD)
                btn.alpha = 1.0f
            } else {
                btn.setTypeface(null, Typeface.NORMAL)
                btn.alpha = 0.6f
            }
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
