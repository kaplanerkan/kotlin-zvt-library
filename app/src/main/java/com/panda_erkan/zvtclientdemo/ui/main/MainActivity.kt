package com.panda_erkan.zvtclientdemo.ui.main

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.navigation.compose.rememberNavController
import com.panda_erkan.zvtclientdemo.ui.components.ConnectionCard
import com.panda_erkan.zvtclientdemo.ui.components.RegistrationConfigDialog
import com.panda_erkan.zvtclientdemo.ui.navigation.BottomNavBar
import com.panda_erkan.zvtclientdemo.ui.navigation.ZvtNavHost
import com.panda_erkan.zvtclientdemo.ui.theme.ZvtClientTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ZvtClientTheme {
                ZvtApp(mainViewModel = mainViewModel)
            }
        }
    }

    companion object {
        fun setAppLocale(languageTag: String) {
            val localeList = LocaleListCompat.forLanguageTags(languageTag)
            AppCompatDelegate.setApplicationLocales(localeList)
        }

        fun getCurrentLanguage(): String {
            val currentLocale = AppCompatDelegate.getApplicationLocales()
            return if (!currentLocale.isEmpty) {
                currentLocale.get(0)?.language ?: "en"
            } else {
                "en"
            }
        }
    }
}

@Composable
private fun ZvtApp(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    var showRegConfig by remember { mutableStateOf(false) }

    if (showRegConfig) {
        RegistrationConfigDialog(
            onDismiss = { showRegConfig = false },
            onSave = { _, _ -> }
        )
    }

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ConnectionCard(
                mainViewModel = mainViewModel,
                onRegConfigClick = { showRegConfig = true },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            ZvtNavHost(
                navController = navController,
                mainViewModel = mainViewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
