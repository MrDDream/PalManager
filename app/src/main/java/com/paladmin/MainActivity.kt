package com.paladmin

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.paladmin.crash.CrashHandler
import com.paladmin.data.local.prefs.AppPreferences
import com.paladmin.data.local.prefs.ThemeMode
import com.paladmin.data.local.prefs.readStoredLanguageBlocking
import com.paladmin.ui.crash.CrashScreen
import com.paladmin.ui.navigation.PalAdminNavHost
import com.paladmin.ui.theme.PalAdminTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var appPreferences: AppPreferences

    override fun attachBaseContext(newBase: Context) {
        val locale = Locale(readStoredLanguageBlocking(newBase).code)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by appPreferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            PalAdminTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var crashTrace by remember { mutableStateOf(CrashHandler.consumeLastCrash(this@MainActivity)) }
                    if (crashTrace != null) {
                        CrashScreen(trace = crashTrace!!, onContinue = { crashTrace = null })
                    } else {
                        val navController = rememberNavController()
                        PalAdminNavHost(navController = navController)
                    }
                }
            }
        }
    }
}
