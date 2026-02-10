package com.panda_erkan.zvtclientdemo.di

import androidx.room.Room
import com.panda.zvt_library.ZvtClient
import com.panda.zvt_library.model.ZvtConfig
import com.panda_erkan.zvtclientdemo.data.AppDatabase
import com.panda_erkan.zvtclientdemo.repository.JournalRepository
import com.panda_erkan.zvtclientdemo.repository.ZvtRepository
import com.panda_erkan.zvtclientdemo.ui.journals.JournalsViewModel
import com.panda_erkan.zvtclientdemo.ui.main.MainViewModel
import com.panda_erkan.zvtclientdemo.ui.payment.PaymentViewModel
import com.panda_erkan.zvtclientdemo.ui.terminal.TerminalViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // ZvtConfig
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

    // Room Database
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "zvt_journal.db"
        ).build()
    }

    // DAO
    single { get<AppDatabase>().journalDao() }

    // Repositories
    single { JournalRepository(get()) }
    single { ZvtRepository(androidContext(), get(), get()) }

    // ViewModels
    viewModel { MainViewModel(androidApplication(), get()) }
    viewModel { PaymentViewModel(androidApplication(), get()) }
    viewModel { TerminalViewModel(androidApplication(), get()) }
    viewModel { JournalsViewModel(androidApplication(), get()) }
}
