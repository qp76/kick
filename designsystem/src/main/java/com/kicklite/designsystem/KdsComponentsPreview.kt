package com.kicklite.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kicklite.designsystem.components.KdsLiveBadge
import com.kicklite.designsystem.components.KdsSectionHeader
import com.kicklite.designsystem.components.KdsSegmentedControl
import com.kicklite.designsystem.components.KdsSurfaceCard

@Preview(name = "KDS dark", showBackground = true)
@Composable
private fun KdsDarkPreview() {
    KickDesignSystemTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KdsSectionHeader(title = "Featured creators")
            KdsLiveBadge(label = "LIVE NOW · 12")
            KdsSegmentedControl(
                options = listOf("Videos", "Clips"),
                selectedIndex = 0,
                onSelectionChanged = {}
            )
            KdsSurfaceCard(Modifier.fillMaxWidth()) {
                Text("Surface card", Modifier.padding(18.dp))
            }
        }
    }
}

@Preview(name = "KDS light", showBackground = true)
@Composable
private fun KdsLightPreview() {
    KickDesignSystemTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KdsSectionHeader(title = "Appearance")
            KdsSurfaceCard(Modifier.fillMaxWidth()) {
                Text("Light surface", Modifier.padding(18.dp))
            }
        }
    }
}
