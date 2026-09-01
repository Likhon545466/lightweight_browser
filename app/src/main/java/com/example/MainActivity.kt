package com.example

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.browser.AppThemeMode
import com.example.browser.BrowserViewModel
import com.example.ui.BrowserScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val browserViewModel: BrowserViewModel = viewModel()
            val themeMode by browserViewModel.themeMode.collectAsStateWithLifecycle()
            val useMaterialYou by browserViewModel.useMaterialYou.collectAsStateWithLifecycle()
            val systemInDark = isSystemInDarkTheme()

            val isDark = when (themeMode) {
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK, AppThemeMode.AMOLED -> true
                AppThemeMode.SYSTEM -> systemInDark
            }

            MyApplicationTheme(
                themeMode = themeMode,
                useMaterialYou = useMaterialYou
            ) {
                BrowserScreen(
                    viewModel = browserViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
