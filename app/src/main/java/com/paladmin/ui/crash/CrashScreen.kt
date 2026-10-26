package com.paladmin.ui.crash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paladmin.R
import com.paladmin.ui.components.BottomActionBar
import com.paladmin.ui.components.IconTile
import com.paladmin.ui.components.PalButton
import com.paladmin.ui.components.PalButtonStyle
import com.paladmin.ui.components.PalCard
import com.paladmin.ui.components.SectionLabel
import com.paladmin.ui.theme.PalTheme

@Composable
fun CrashScreen(trace: String, onContinue: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val c = PalTheme.colors

    Scaffold(
        containerColor = c.background,
        bottomBar = {
            BottomActionBar {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PalButton(
                        text = stringResource(R.string.crash_continue),
                        onClick = onContinue,
                        style = PalButtonStyle.Neutral,
                        height = 52.dp,
                        modifier = Modifier.weight(1f),
                    )
                    PalButton(
                        text = stringResource(R.string.crash_copy),
                        onClick = { clipboard.setText(AnnotatedString(trace)) },
                        icon = Icons.Filled.ContentCopy,
                        height = 52.dp,
                        modifier = Modifier.weight(1.4f),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconTile(Icons.Filled.BugReport, tone = c.danger, size = 44.dp, iconSize = 24.dp, radius = 14.dp)
                Text(
                    stringResource(R.string.crash_title),
                    style = PalTheme.type.screenTitle,
                    color = c.text,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
            }
            Text(stringResource(R.string.crash_subtitle), style = PalTheme.type.body, color = c.textSecondary)
            SectionLabel(stringResource(R.string.servers_crash_trace))
            PalCard(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                background = c.surfaceBar,
                contentPadding = PaddingValues(0.dp),
                spacing = 0.dp,
            ) {
                Text(
                    trace,
                    style = PalTheme.type.mono.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                    color = c.textBody,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                )
            }
        }
    }
}
