package com.paladmin.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.paladmin.R
import com.paladmin.ui.theme.PalTheme

@Composable
fun SplashScreen(
    onOpenDashboard: (Long) -> Unit,
    onOpenProfileList: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val destination by viewModel.destination.collectAsState()

    LaunchedEffect(destination) {
        when (val current = destination) {
            is SplashDestination.OpenDashboard -> onOpenDashboard(current.profileId)
            SplashDestination.OpenProfileList -> onOpenProfileList()
            SplashDestination.Loading -> Unit
        }
    }

    SplashContent()
}

/** Écran d'attente : logo + nom de l'app, le temps de choisir la destination de départ. */
@Composable
fun SplashContent() {
    val c = PalTheme.colors
    val loading = stringResource(R.string.servers_splash_loading)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(c.background).semantics { contentDescription = loading },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = c.green.content,
                modifier = Modifier.size(112.dp),
            )
            Text(stringResource(R.string.servers_brand), style = PalTheme.type.brand.copy(fontSize = 28.sp), color = c.text)
            CircularProgressIndicator(
                color = c.green.solid,
                trackColor = c.neutral.container,
                strokeWidth = 3.dp,
                modifier = Modifier.padding(top = 24.dp).size(28.dp),
            )
        }
    }
}
