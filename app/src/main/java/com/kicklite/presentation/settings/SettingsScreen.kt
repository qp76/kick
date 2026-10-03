package com.kicklite.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kicklite.R
import com.kicklite.domain.model.ThemePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: ThemeViewModel = hiltViewModel()
) {
    val themePreference by viewModel.themePreference.collectAsState()
    val themeOptions = listOf(
        ThemePreference.AUTO,
        ThemePreference.LIGHT,
        ThemePreference.DARK
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_theme_section_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.settings_theme_section_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(themeOptions) { option ->
                val title = when (option) {
                    ThemePreference.AUTO -> stringResource(R.string.settings_theme_option_auto)
                    ThemePreference.LIGHT -> stringResource(R.string.settings_theme_option_light)
                    ThemePreference.DARK -> stringResource(R.string.settings_theme_option_dark)
                }
                val description = when (option) {
                    ThemePreference.AUTO -> stringResource(R.string.settings_theme_option_auto_description)
                    ThemePreference.LIGHT -> stringResource(R.string.settings_theme_option_light_description)
                    ThemePreference.DARK -> stringResource(R.string.settings_theme_option_dark_description)
                }

                ThemeOptionRow(
                    title = title,
                    description = description,
                    isSelected = themePreference == option,
                    onSelected = { viewModel.updateThemePreference(option) }
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    description: String,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onSelected),
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            RadioButton(
                selected = isSelected,
                onClick = null
            )
        }
    )
}
