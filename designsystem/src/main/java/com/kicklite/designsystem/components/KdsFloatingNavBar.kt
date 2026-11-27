package com.kicklite.designsystem.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kicklite.designsystem.tokens.KdsColor

/**
 * Floating, translucent navigation capsule for the phone experience.
 * The selected destination expands into a branded glass pill.
 */
@Composable
fun KdsFloatingNavBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem("home", "Home", Icons.Default.Home),
        NavItem("search", "Search", Icons.Default.Search),
        NavItem("following", "Following", Icons.Default.Favorite)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xE8242B27),
                        Color(0xF20E120F)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(30.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val selected = selectedTab == item.route
            Row(
                modifier = Modifier
                    .weight(if (selected) 1.35f else 1f)
                    .animateContentSize(spring(dampingRatio = 0.78f, stiffness = 520f))
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (selected) Brush.linearGradient(
                            listOf(
                                KdsColor.Brand.copy(alpha = 0.22f),
                                Color.White.copy(alpha = 0.055f)
                            )
                        ) else Brush.linearGradient(
                            listOf(Color.Transparent, Color.Transparent)
                        )
                    )
                    .border(
                        width = if (selected) 1.dp else 0.dp,
                        color = if (selected) KdsColor.Brand.copy(alpha = 0.26f) else Color.Transparent,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onTabSelected(item.route) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    modifier = Modifier.size(20.dp),
                    tint = if (selected) KdsColor.Brand else Color(0xFF9BA69F)
                )
                if (selected) {
                    Text(
                        text = item.label,
                        modifier = Modifier.padding(start = 8.dp),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
