package com.kicklite.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val dataSaverVm: DataSaverViewModel = hiltViewModel()
    val dataSaver by dataSaverVm.prefs.enabled.collectAsState()
    val previews by dataSaverVm.prefs.previews.collectAsState()
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

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.settings_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                KdsSectionHeader(title = stringResource(R.string.settings_theme_section_title))
                KdsSurfaceCard(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp)) {
                        PreferenceSwitchRow(
                            title = "Data saver",
                            description = "Uses a lower stream quality when available to reduce mobile data.",
                            checked = dataSaver,
                            onCheckedChange = { dataSaverVm.prefs.setEnabled(it) }
                        )
                        PreferenceSwitchRow(
                            title = "Live previews",
                            description = "Allow muted live previews in native discovery screens.",
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
    androidx.compose.foundation.layout.Row(
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
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
