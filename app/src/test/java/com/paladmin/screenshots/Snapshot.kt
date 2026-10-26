package com.paladmin.screenshots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import app.cash.paparazzi.Paparazzi
import com.paladmin.ui.theme.PalAdminTheme

/**
 * Capture d'un écran réel dans le thème de l'app. Le contexte Paparazzi (qui expose les assets) et
 * le mode inspection sont fournis explicitement, pour que les images locales se chargent de façon
 * synchrone (voir FallbackAsyncImage).
 */
fun Paparazzi.snapshotScreen(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val ctx = context
    snapshot {
        CompositionLocalProvider(LocalContext provides ctx, LocalInspectionMode provides true) {
            PalAdminTheme(darkTheme = darkTheme) { content() }
        }
    }
}
