package com.kicklite.presentation.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kicklite.R

data class Language(
    val code: String?,
    val name: String,
    val flag: String
)

@Composable
fun LanguageFilter(
    selectedLanguage: String?,
    onLanguageSelected: (String?) -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val languages = listOf(
        Language(null, stringResource(R.string.language_all), "🌍"),
        Language("tr", stringResource(R.string.language_name_turkish), "🇹🇷"),
        Language("en", stringResource(R.string.language_name_english), "🇺🇸"),
        Language("es", stringResource(R.string.language_name_spanish), "🇪🇸"),
        Language("de", stringResource(R.string.language_name_german), "🇩🇪"),
        Language("fr", stringResource(R.string.language_name_french), "🇫🇷"),
        Language("pt", stringResource(R.string.language_name_portuguese), "🇵🇹"),
        Language("ru", stringResource(R.string.language_name_russian), "🇷🇺"),
        Language("ja", stringResource(R.string.language_name_japanese), "🇯🇵"),
        Language("ko", stringResource(R.string.language_name_korean), "🇰🇷")
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header with clear button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.filter_by_language_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (selectedLanguage != null) {
                IconButton(
                    onClick = onClearFilter,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(R.string.clear_filter),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Language chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(languages) { language ->
                LanguageChip(
                    language = language,
                    isSelected = selectedLanguage == language.code,
                    onClick = { onLanguageSelected(language.code) }
                )
            }
        }
    }
}

@Composable
private fun LanguageChip(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Stable custom chip implementation
    val containerColor = if (isSelected) com.kicklite.ui.theme.KickGreen else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 32.dp)
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = language.flag)
            Text(
                text = language.name,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
