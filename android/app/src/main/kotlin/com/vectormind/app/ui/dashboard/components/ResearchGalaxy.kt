package com.vectorminds.app.ui.dashboard.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vectorminds.app.ui.theme.*
import kotlin.random.Random

data class GalaxyPoint(
    val x: Float,
    val y: Float,
    val label: String,
    val category: String,
    val score: Float,
    val neighbors: List<Int> = emptyList() // Indices of neighboring points
)

@Composable
fun ResearchGalaxy(
    points: List<GalaxyPoint>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "galaxy")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF050510))
    ) {
        // Starfield background
        Starfield()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2
            val centerY = height / 2

            // Draw Relationship Lines first (under points)
            points.forEachIndexed { index, point ->
                point.neighbors.forEach { neighborIdx ->
                    if (neighborIdx < points.size) {
                        val neighbor = points[neighborIdx]
                        drawLine(
                            color = CyanPrimary.copy(alpha = 0.15f * pulse),
                            start = Offset(centerX + point.x * centerX * 0.8f, centerY + point.y * centerY * 0.8f),
                            end = Offset(centerX + neighbor.x * centerX * 0.8f, centerY + neighbor.y * centerY * 0.8f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }

            // Draw Points
            points.forEach { point ->
                val xPos = centerX + point.x * centerX * 0.8f
                val yPos = centerY + point.y * centerY * 0.8f
                
                val color = when (point.category.lowercase()) {
                    "vision" -> PurpleSecondary
                    "nlp" -> CyanPrimary
                    "audio" -> EmeraldTertiary
                    else -> WarningAmber
                }

                // Outer glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.4f * pulse), Color.Transparent),
                        center = Offset(xPos, yPos),
                        radius = 12.dp.toPx()
                    ),
                    radius = 12.dp.toPx(),
                    center = Offset(xPos, yPos)
                )

                // Core point
                drawCircle(
                    color = color,
                    radius = (3f + (point.score * 2f)).dp.toPx(),
                    center = Offset(xPos, yPos)
                )
            }
        }

        // Overlay Legend
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
        ) {
            LegendItem("LLM Evolution", CyanPrimary)
            LegendItem("Diffusion Models", PurpleSecondary)
        }
        
        Text(
            "LIVE PROJECTION",
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, fontSize = 10.sp, color = Color.LightGray)
    }
}

@Composable
fun Starfield() {
    // Static stars — no infinite transition. The galaxy already has a
    // pulse animation; running a second one for ~50 stars at ~60fps was
    // wasteful and caused first-frame jank on slow emulators.
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rng = Random(42)
        repeat(40) {
            val x = rng.nextFloat() * size.width
            val y = rng.nextFloat() * size.height
            val starAlpha = 0.3f + (rng.nextFloat() * 0.4f)
            drawCircle(
                Color.White.copy(alpha = starAlpha),
                radius = 1.dp.toPx(),
                center = Offset(x, y),
            )
        }
    }
}

fun generateMockGalaxyPoints(): List<GalaxyPoint> {
    return listOf(
        GalaxyPoint(0.2f, -0.3f, "GPT-4o", "nlp", 0.9f, listOf(1, 2)),
        GalaxyPoint(0.4f, -0.1f, "Llama-3", "nlp", 0.85f, listOf(0)),
        GalaxyPoint(-0.3f, 0.5f, "Stable Diffusion 3", "vision", 0.95f, listOf(4)),
        GalaxyPoint(0.1f, 0.2f, "Mistral", "nlp", 0.7f, listOf(0)),
        GalaxyPoint(-0.5f, 0.2f, "DALL-E 3", "vision", 0.8f, listOf(2)),
        GalaxyPoint(0.6f, 0.4f, "Sora", "video", 0.99f),
        GalaxyPoint(-0.2f, -0.6f, "AudioCraft", "audio", 0.6f)
    )
}
