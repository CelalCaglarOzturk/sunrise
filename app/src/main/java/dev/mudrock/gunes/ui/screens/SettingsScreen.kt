package dev.mudrock.gunes.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.mudrock.gunes.R
import dev.mudrock.gunes.ui.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            uri?.let {
                try {
                    context.contentResolver.openOutputStream(it)?.let { stream ->
                        viewModel.backupSettings(stream)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.let { stream ->
                    viewModel.restoreSettings(stream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text(
            stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Clock Mode
        Text(
            stringResource(R.string.default_clock_mode),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.updateSettings { s -> s.copy(defaultClockMode = "normal") } },
                colors = if (settings.defaultClockMode == "normal") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.normal_clock))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { viewModel.updateSettings { s -> s.copy(defaultClockMode = "sun") } },
                colors = if (settings.defaultClockMode == "sun") ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.sun_based_clock))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.use_24_hour_format))
            Switch(
                checked = settings.use24HourFormat,
                onCheckedChange = { viewModel.updateSettings { s -> s.copy(use24HourFormat = it) } }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.show_seconds))
            Switch(
                checked = settings.showSeconds,
                onCheckedChange = { viewModel.updateSettings { s -> s.copy(showSeconds = it) } }
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Permissions
        Text(
            stringResource(R.string.permissions),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Battery Opt
        OutlinedButton(onClick = {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
        }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.battery_optimization))
        }
        Text(
            stringResource(R.string.battery_optimization_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // System Alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            OutlinedButton(onClick = {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                context.startActivity(intent)
            }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.system_alarm_access))
            }
            Text(
                stringResource(R.string.system_alarm_access_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // Backup
        Button(onClick = { exportLauncher.launch("sunrise_backup.json") }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Download, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.export_settings))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Upload, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.import_settings))
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        // About
        Text(
            stringResource(R.string.about),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text("${stringResource(R.string.app_name)} v1.0.0", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(32.dp))
    }
}
