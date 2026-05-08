package com.vectormind.app.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReasoningPanel(
    contextLabel: String,
    confidence: Float,
    reasoningPoints: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E2E).copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Context: $contextLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Cyan
                )
                Text(
                    text = "${(confidence * 100).toInt()}% Confidence",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = confidence,
                modifier = Modifier.fillMaxWidth(),
                color = Color.Cyan,
                trackColor = Color.DarkGray
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = "Why I acted:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            reasoningPoints.forEach { point ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp).padding(top = 2.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = point,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
