package com.example.ui.components

import android.view.MotionEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun GlassTelemetryCard(
    modifier: Modifier = Modifier,
    borderColor: Color = OutlineSubtle.copy(alpha = 0.7f),
    containerColor: Color = DeepSpace.copy(alpha = 0.92f),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        content()
    }
}

@Composable
fun SectionTitleRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color = ElectricCyan,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.14f))
                    .border(1.dp, iconTint.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun PerformanceScoreRing(
    score: Int,
    label: String,
    size: Dp = 136.dp,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (score.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 700),
        label = "scoreRing"
    )
    val accentColor = when {
        score >= 85 -> MintEmerald
        score >= 65 -> ElectricCyan
        score >= 50 -> SolarAmber
        else -> CoralCrimson
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 11.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            drawArc(
                color = ElevatedSlate,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(ElectricCyan, accentColor, ElectricCyan)
                ),
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun MetricPill(
    label: String,
    value: String,
    accent: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = ElevatedSlate.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ThermalArcGauge(
    temperatureCelsius: Float,
    alertThresholdCelsius: Float,
    modifier: Modifier = Modifier
) {
    val normalized = ((temperatureCelsius - 20f) / 35f).coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = normalized,
        animationSpec = tween(durationMillis = 500),
        label = "thermalSweep"
    )
    val tempColor = when {
        temperatureCelsius >= alertThresholdCelsius -> CoralCrimson
        temperatureCelsius >= 37f -> SolarAmber
        temperatureCelsius >= 33f -> ElectricCyan
        else -> MintEmerald
    }

    Box(
        modifier = modifier.size(180.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 14.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)

            drawArc(
                color = ElevatedSlate,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.horizontalGradient(
                    colors = listOf(MintEmerald, ElectricCyan, tempColor)
                ),
                startAngle = 140f,
                sweepAngle = 260f * animatedSweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.1f°C", temperatureCelsius),
                style = MaterialTheme.typography.displaySmall,
                color = tempColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when {
                    temperatureCelsius >= alertThresholdCelsius -> "PANAS • PERLU DINGIN"
                    temperatureCelsius >= 37f -> "HANGAT • PANTAU"
                    temperatureCelsius >= 33f -> "SUHU OPTIMAL"
                    else -> "DINGIN & STABIL"
                },
                style = MaterialTheme.typography.labelSmall,
                color = tempColor
            )
        }
    }
}

@Composable
fun ThermalHistoryChart(
    history: List<Float>,
    thresholdCelsius: Float,
    modifier: Modifier = Modifier
) {
    val points = if (history.isEmpty()) listOf(32f, 32f) else if (history.size == 1) listOf(history[0], history[0]) else history
    val minTemp = (points.minOrNull()?.minus(2f) ?: 25f).coerceAtMost(25f)
    val maxTemp = (maxOf(points.maxOrNull() ?: 45f, thresholdCelsius) + 2f).coerceAtLeast(45f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianVoid.copy(alpha = 0.7f))
                .padding(horizontal = 10.dp, vertical = 12.dp)
        ) {
            val w = size.width
            val h = size.height
            val range = (maxTemp - minTemp).coerceAtLeast(1f)

            // Threshold dashed line
            val thresholdY = h - (((thresholdCelsius - minTemp) / range) * h).coerceIn(0f, h)
            drawLine(
                color = CoralCrimson.copy(alpha = 0.55f),
                start = Offset(0f, thresholdY),
                end = Offset(w, thresholdY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )

            val stepX = if (points.size > 1) w / (points.size - 1) else w
            val linePath = Path()
            val fillPath = Path()

            points.forEachIndexed { index, temp ->
                val x = index * stepX
                val y = h - (((temp - minTemp) / range) * h).coerceIn(0f, h)
                if (index == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo((points.size - 1) * stepX, h)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ElectricCyan.copy(alpha = 0.35f),
                        Color.Transparent
                    )
                )
            )
            drawPath(
                path = linePath,
                color = ElectricCyan,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            val lastTemp = points.last()
            val lastY = h - (((lastTemp - minTemp) / range) * h).coerceIn(0f, h)
            val dotColor = if (lastTemp >= thresholdCelsius) CoralCrimson else MintEmerald
            drawCircle(
                color = dotColor,
                radius = 5.dp.toPx(),
                center = Offset((points.size - 1) * stepX, lastY)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Min: ${String.format("%.1f", points.minOrNull() ?: 0f)}°C",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = "Batas Alert: ${thresholdCelsius.roundToInt()}°C",
                style = MaterialTheme.typography.labelSmall,
                color = CoralCrimson
            )
            Text(
                text = "Max: ${String.format("%.1f", points.maxOrNull() ?: 0f)}°C",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TouchSmoothnessPad(
    modifier: Modifier = Modifier
) {
    val trailPoints = remember { mutableStateListOf<Offset>() }
    var lastEventNanos by remember { mutableLongStateOf(0L) }
    var touchRateHz by remember { mutableFloatStateOf(0f) }
    var avgLatencyMs by remember { mutableFloatStateOf(0f) }
    var sampleCount by remember { mutableIntStateOf(0) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ObsidianVoid)
                .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .testTag("touch_smoothness_pad")
                .pointerInteropFilter { event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            trailPoints.clear()
                            trailPoints.add(Offset(event.x, event.y))
                            lastEventNanos = System.nanoTime()
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val now = System.nanoTime()
                            val deltaNs = now - lastEventNanos
                            if (deltaNs > 500_000L) {
                                val hz = (1_000_000_000f / deltaNs).coerceIn(15f, 1000f)
                                val ms = deltaNs / 1_000_000f
                                touchRateHz = if (touchRateHz == 0f) hz else (touchRateHz * 0.7f + hz * 0.3f)
                                avgLatencyMs = if (avgLatencyMs == 0f) ms else (avgLatencyMs * 0.7f + ms * 0.3f)
                                sampleCount++
                            }
                            lastEventNanos = now
                            // Include historical batch points for true high-Hz touch sampling visibility
                            for (h in 0 until event.historySize) {
                                trailPoints.add(Offset(event.getHistoricalX(h), event.getHistoricalY(h)))
                            }
                            trailPoints.add(Offset(event.x, event.y))
                            while (trailPoints.size > 80) {
                                trailPoints.removeAt(0)
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            // Keep trail briefly visible
                        }
                    }
                    true
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Subtle calibration grid
                val cols = 6
                val rows = 4
                for (c in 1 until cols) {
                    val x = size.width * (c / cols.toFloat())
                    drawLine(
                        color = OutlineSubtle.copy(alpha = 0.25f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                for (r in 1 until rows) {
                    val y = size.height * (r / rows.toFloat())
                    drawLine(
                        color = OutlineSubtle.copy(alpha = 0.25f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (trailPoints.size >= 2) {
                    for (i in 1 until trailPoints.size) {
                        val alpha = i.toFloat() / trailPoints.size.toFloat()
                        drawLine(
                            color = ElectricCyan.copy(alpha = alpha),
                            start = trailPoints[i - 1],
                            end = trailPoints[i],
                            strokeWidth = (2.dp + (3.dp * alpha)).toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    drawCircle(
                        color = MintEmerald,
                        radius = 7.dp.toPx(),
                        center = trailPoints.last()
                    )
                }
            }

            if (trailPoints.isEmpty()) {
                Text(
                    text = "Geser jari di sini untuk uji kelicinan & respons sentuh",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = ElevatedSlate
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Touch Sampling", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = if (touchRateHz > 0f) "${touchRateHz.roundToInt()} Hz" else "-- Hz",
                        style = MaterialTheme.typography.titleMedium,
                        color = MintEmerald
                    )
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                color = ElevatedSlate
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Interval Sentuh", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(
                        text = if (avgLatencyMs > 0f) String.format("%.1f ms", avgLatencyMs) else "-- ms",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricCyan
                    )
                }
            }
        }
    }
}
