package com.kicklite.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.kicklite.R
import androidx.compose.ui.unit.dp

@Composable
fun ErrorMessage(
    message: String,
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        
        onRetryClick?.let { retry ->
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = retry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
