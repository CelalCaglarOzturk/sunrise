package dev.mudrock.gunes.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.mudrock.gunes.R
import dev.mudrock.gunes.ui.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        SettingsSection(title = stringResource(R.string.default_clock_mode)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToggleButton(
                    label = stringResource(R.string.normal_clock),
                    icon = Icons.Default.Schedule,
                    selected = settings.defaultClockMode == "normal",
                    onClick = { viewModel.updateSettings { s -> s.copy(defaultClockMode = "normal") } },
                    modifier = Modifier.weight(1f)
                )
                ToggleButton(
                    label = stringResource(R.string.sun_based_clock),
                    icon = Icons.Default.WbSunny,
                    selected = settings.defaultClockMode == "sun",
                    onClick = { viewModel.updateSettings { s -> s.copy(defaultClockMode = "sun") } },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SettingsSection(title = stringResource(R.string.use_24_hour_format)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToggleButton(
                    label = "12 Hour",
                    selected = !settings.use24HourFormat,
                    onClick = { viewModel.updateSettings { s -> s.copy(use24HourFormat = false) } },
                    modifier = Modifier.weight(1f)
                )
                ToggleButton(
                    label = "24 Hour",
                    selected = settings.use24HourFormat,
                    onClick = { viewModel.updateSettings { s -> s.copy(use24HourFormat = true) } },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        SettingsSection(title = stringResource(R.string.show_seconds)) {
            SettingsToggleRow(
                label = stringResource(R.string.show_seconds),
                checked = settings.showSeconds,
                onCheckedChange = { viewModel.updateSettings { s -> s.copy(showSeconds = it) } }
            )
        }

        // Language - English only
        SettingsSection(title = stringResource(R.string.language)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.lang_english),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        SettingsSection(title = stringResource(R.string.permissions)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionButton(
                    label = stringResource(R.string.battery_optimization),
                    description = stringResource(R.string.battery_optimization_desc),
                    onClick = {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PermissionButton(
                        label = stringResource(R.string.system_alarm_access),
                        description = stringResource(R.string.system_alarm_access_desc),
                        onClick = {
                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }

        SettingsSection(title = "${stringResource(R.string.export_settings)} / ${stringResource(R.string.import_settings)}") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { exportLauncher.launch("sunrise_backup.json") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.export_settings))
                }
                Button(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.import_settings))
                }
            }
        }

        SettingsSection(title = stringResource(R.string.about)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Sunrise Clock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
fun ToggleButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (selected) Color.Black else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun PermissionButton(
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(stringResource(R.string.grant_permission), fontWeight = FontWeight.Bold)
            }
        }
    }
}
