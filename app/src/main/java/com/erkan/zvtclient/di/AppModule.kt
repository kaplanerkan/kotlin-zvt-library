package com.erkan.zvtclient.di

import com.erkan.zvt.ZvtClient
import com.erkan.zvt.model.ZvtConfig
import com.erkan.zvtclient.repository.ZvtRepository
import com.erkan.zvtclient.ui.main.MainViewModel
import com.erkan.zvtclient.ui.payment.PaymentViewModel
import com.erkan.zvtclient.ui.terminal.TerminalViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ZvtConfig - SharedPreferences'tan okunabilir, şimdilik varsayılan
    single {
        val prefs = androidContext().getSharedPreferences("zvt_settings", 0)
        ZvtConfig(
            host = prefs.getString("terminal_host", "192.168.1.100") ?: "192.168.1.100",
            port = prefs.getInt("terminal_port", 20007),
            password = prefs.getString("terminal_password", "000000") ?: "000000",
            currencyCode = prefs.getInt("currency_code", 978),
            debugMode = prefs.getBoolean("debug_mode", true)
        )
    }

    // ZvtClient - Singleton
    single { ZvtClient(get()) }

    // Repository
    single { ZvtRepository(get()) }

    // ViewModels
    viewModel { MainViewModel(get()) }
    viewModel { PaymentViewModel(get()) }
    viewModel { TerminalViewModel(get()) }
}
