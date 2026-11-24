package com.kicklite.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kicklite.R
import com.kicklite.designsystem.components.KdsSectionHeader
import com.kicklite.designsystem.components.KdsSegmentedControl
import com.kicklite.designsystem.components.KdsSurfaceCard
import com.kicklite.domain.model.ThemePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ThemeViewModel = hiltViewModel()
) {
    val themePreference by viewModel.themePreference.collectAsState()
    val authViewModel: KickAuthViewModel = hiltViewModel()
    val dataSaverVm: DataSaverViewModel = hiltViewModel()
    val context = LocalContext.current
    val apiConfigured by authViewModel.configured.collectAsState()
    val dataSaver by dataSaverVm.prefs.enabled.collectAsState()
    val previews by dataSaverVm.prefs.previews.collectAsState()
    var tokenInput by remember { mutableStateOf("") }
    var loginMessage by remember { mutableStateOf<String?>(null) }

    val themeOptions = listOf(
        stringResource(R.string.settings_theme_option_auto),
        stringResource(R.string.settings_theme_option_light),
        stringResource(R.string.settings_theme_option_dark)
    )
    val selectedThemeIndex = when (themePreference) {
        ThemePreference.AUTO -> 0
        ThemePreference.LIGHT -> 1
        ThemePreference.DARK -> 2
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 18.dp,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                KdsSectionHeader(title = stringResource(R.string.settings_account_section_title))
                KdsSurfaceCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("KICK API", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (apiConfigured) "KICK access token is stored on this device."
                            else "Sign in with KICK to connect your account, or provide a development access token.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        loginMessage?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        }
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Access token") },
                            supportingText = { Text("Never commit access tokens or client secrets to GitHub.") }
                        )
                        Button(
                            onClick = {
                                loginMessage = if (authViewModel.startLogin(context)) {
                                    "Opening Kick sign-in… Complete authorization in your browser and return to the app."
                                } else {
                                    "Login could not start. Check the KICK_OAUTH_GATEWAY_URL build setting."
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Log in with KICK") }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    authViewModel.saveToken(tokenInput)
                                    tokenInput = ""
                                    loginMessage = "Access token saved on this device."
                                },
                                enabled = tokenInput.isNotBlank()
                            ) { Text("Save token") }
                            if (apiConfigured) {
                                OutlinedButton(onClick = {
                                    authViewModel.clearToken()
                                    loginMessage = "Access token cleared."
                                }) { Text("Clear") }
                            }
                        }
                    }
                }
            }

            item {
                KdsSectionHeader(title = stringResource(R.string.settings_theme_section_title))
                KdsSurfaceCard(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            stringResource(R.string.settings_theme_section_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        KdsSegmentedControl(
                            options = themeOptions,
                            selectedIndex = selectedThemeIndex,
                            onSelectionChanged = { index ->
                                viewModel.updateThemePreference(
                                    when (index) {
                                        1 -> ThemePreference.LIGHT
                                        2 -> ThemePreference.DARK
                                        else -> ThemePreference.AUTO
                                    }
                                )
                            }
                        )
                    }
                }
            }

            item {
                KdsSectionHeader(title = stringResource(R.string.settings_playback_data_section_title))
                KdsSurfaceCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                        PreferenceSwitchRow(
                            title = "Data saver",
                            description = "Limits Auto video quality to 480p to use less mobile data.",
                            checked = dataSaver,
                            onCheckedChange = { dataSaverVm.prefs.setEnabled(it) }
                        )
                        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
                        PreferenceSwitchRow(
                            title = "Live previews",
                            description = "Plays a muted preview on Home when a stream rests on screen. Off while Data saver is on.",
                            checked = previews,
                            onCheckedChange = { dataSaverVm.prefs.setPreviews(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(1.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
