package com.paladmin.ui.crash

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.paladmin.R

@Composable
fun CrashScreen(trace: String, onContinue: () -> Unit) {
    val clipboard = LocalClipboardManager.current

    Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(stringResource(R.string.crash_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.crash_subtitle),
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(trace, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
            }
            Column(modifier = Modifier.padding(top = 12.dp)) {
                Button(
                    onClick = { clipboard.setText(AnnotatedString(trace)) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.crash_copy)) }
                OutlinedButton(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                ) { Text(stringResource(R.string.crash_continue)) }
            }
        }
    }
}
