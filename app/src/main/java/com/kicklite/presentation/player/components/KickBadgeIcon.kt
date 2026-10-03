package com.kicklite.presentation.player.components

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.request.ImageRequest

/** Lists badge files the user dropped into app/src/main/assets/badges/ (e.g. moderator.svg). */
private object BadgeAssets {
    @Volatile private var cached: Set<String>? = null

    fun has(context: Context, type: String): Boolean {
        val set = cached ?: (context.assets.list("badges")?.map { it.substringBeforeLast('.') }?.toSet() ?: emptySet())
            .also { cached = it }
        return type in set
    }

    fun fileFor(context: Context, type: String): String? =
        context.assets.list("badges")?.firstOrNull { it.substringBeforeLast('.') == type }
}

/**
 * Badge before the username. If a file badges/<type>.svg|png|webp exists in assets it is used
 * (that is how you plug in Kick's original artwork); otherwise a lookalike is drawn in code.
 */
@Composable
fun KickBadgeIcon(type: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val assetFile = remember(type) {
        if (BadgeAssets.has(context, type)) BadgeAssets.fileFor(context, type) else null
    }
    if (assetFile != null) {
        val request = remember(assetFile) {
            ImageRequest.Builder(context)
                .data("file:///android_asset/badges/$assetFile")
                .decoderFactory(SvgDecoder.Factory())
                .build()
        }
        AsyncImage(model = request, contentDescription = type, modifier = modifier)
    } else {
        Canvas(modifier = modifier.fillMaxSize()) { drawLookalike(type) }
    }
}

fun isKnownBadge(type: String): Boolean = type in KNOWN_BADGES

private val KNOWN_BADGES = setOf(
    "broadcaster", "moderator", "vip", "og", "founder", "verified",
    "subscriber", "sub_gifter", "staff", "sidekick"
)

private fun DrawScope.poly(points: List<Pair<Float, Float>>, color: Color) {
    val u = size.minDimension / 24f
    val p = Path()
    points.forEachIndexed { i, (x, y) ->
        if (i == 0) p.moveTo(x * u, y * u) else p.lineTo(x * u, y * u)
    }
    p.close()
    drawPath(p, color)
}

private fun DrawScope.drawLookalike(type: String) {
    val u = size.minDimension / 24f
    when (type) {
        "broadcaster" -> {
            drawRoundRect(Color(0xFFE5383B), size = size, cornerRadius = CornerRadius(5f * u))
            drawCircle(Color.White, radius = 5f * u, center = Offset(12f * u, 12f * u), style = Stroke(2.2f * u))
            drawCircle(Color.White, radius = 2.2f * u, center = Offset(12f * u, 12f * u))
        }
        "moderator" -> {
            drawRoundRect(Color(0xFF53FC18), size = size, cornerRadius = CornerRadius(5f * u))
            // sword
            drawLine(Color.Black, Offset(7f * u, 17f * u), Offset(17f * u, 7f * u), 2.6f * u, StrokeCap.Round)
            drawLine(Color.Black, Offset(8f * u, 11f * u), Offset(13f * u, 16f * u), 2.2f * u, StrokeCap.Round)
            drawLine(Color.Black, Offset(6f * u, 18f * u), Offset(8f * u, 16f * u), 2.2f * u, StrokeCap.Round)
        }
        "vip" -> poly(listOf(12f to 1.5f, 22f to 9f, 12f to 22.5f, 2f to 9f), Color(0xFFFF4DB8))
        "og" -> poly(
            listOf(2f to 19f, 3f to 7f, 8f to 12f, 12f to 4f, 16f to 12f, 21f to 7f, 22f to 19f),
            Color(0xFFFFB300)
        )
        "founder" -> {
            drawCircle(Color(0xFFFF8F00), radius = 11f * u, center = Offset(12f * u, 12f * u))
            poly(
                listOf(12f to 5f, 14f to 10f, 19f to 10f, 15f to 13.5f, 16.5f to 19f, 12f to 15.8f,
                    7.5f to 19f, 9f to 13.5f, 5f to 10f, 10f to 10f),
                Color.White
            )
        }
        "verified" -> {
            drawCircle(Color(0xFF1FA33A), radius = 11f * u, center = Offset(12f * u, 12f * u))
            drawLine(Color.White, Offset(7f * u, 12.5f * u), Offset(10.5f * u, 16f * u), 2.4f * u, StrokeCap.Round)
            drawLine(Color.White, Offset(10.5f * u, 16f * u), Offset(17f * u, 8f * u), 2.4f * u, StrokeCap.Round)
        }
        "subscriber" -> poly(
            listOf(12f to 1.5f, 15f to 8.5f, 22.5f to 9f, 16.8f to 14f, 18.6f to 21.5f, 12f to 17.5f,
                5.4f to 21.5f, 7.2f to 14f, 1.5f to 9f, 9f to 8.5f),
            Color(0xFF3DDC12)
        )
        "sub_gifter" -> {
            drawRoundRect(Color(0xFF8E4DFF), size = size, cornerRadius = CornerRadius(5f * u))
            drawLine(Color.White, Offset(12f * u, 6f * u), Offset(12f * u, 18f * u), 2.6f * u, StrokeCap.Round)
            drawLine(Color.White, Offset(6f * u, 12f * u), Offset(18f * u, 12f * u), 2.6f * u, StrokeCap.Round)
        }
        "staff" -> {
            drawRoundRect(Color(0xFF53FC18), size = size, cornerRadius = CornerRadius(5f * u))
            drawLine(Color.Black, Offset(8f * u, 6f * u), Offset(8f * u, 18f * u), 3f * u, StrokeCap.Butt)
            drawLine(Color.Black, Offset(9f * u, 12f * u), Offset(16f * u, 6f * u), 3f * u, StrokeCap.Butt)
            drawLine(Color.Black, Offset(9f * u, 12f * u), Offset(16f * u, 18f * u), 3f * u, StrokeCap.Butt)
        }
        else -> drawRoundRect(Color(0xFF7A8A80), size = Size(size.width, size.height), cornerRadius = CornerRadius(5f * u))
    }
}
