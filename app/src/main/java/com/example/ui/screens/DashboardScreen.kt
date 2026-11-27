package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.formatBytes
import com.example.core.formatGigabytes
import com.example.core.formatTimestamp
import com.example.model.AiResponseState
import com.example.model.AutoCleanInterval
import com.example.model.BatteryTelemetry
import com.example.model.DisplayTelemetry
import com.example.model.JunkScanResult
import com.example.model.OptimizerSettings
import com.example.model.RamTelemetry
import com.example.model.StorageTelemetry
import com.example.ui.components.GlassTelemetryCard
import com.example.ui.components.MetricPill
import com.example.ui.components.PerformanceScoreRing
import com.example.ui.components.SectionTitleRow
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.DeepSpace
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DashboardScreen(
    performanceScore: Int,
    ram: RamTelemetry,
    storage: StorageTelemetry,
    junk: JunkScanResult,
    battery: BatteryTelemetry,
    display: DisplayTelemetry,
    liveFps: Float,
    settings: OptimizerSettings,
    isMasterBoosting: Boolean,
    quickPulseState: AiResponseState,
    onMasterBoost: () -> Unit,
    onScanJunk: () -> Unit,
    onCleanJunk: () -> Unit,
    onToggleJunkCategory: (String) -> Unit,
    onSelectAutoCleanInterval: (AutoCleanInterval) -> Unit,
    onRunQuickAiPulse: () -> Unit,
    onOpenAiAdvisor: () -> Unit,
    onOpenSystemStorageCleaner: () -> Intent,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            GlassTelemetryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                borderColor = ElectricCyan.copy(alpha = 0.35f)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_core_banner_1795796520524),
                        contentDescription = stringResource(id = R.string.hero_banner_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .alpha(0.22f)
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ObsidianVoid.copy(alpha = 0.35f),
                                        DeepSpace.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = CyanContainer
                                ) {
                                    Text(
                                        text = "PULSEBOOST ENGINE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ElectricCyan,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = when {
                                        performanceScore >= 85 -> "Performa Prima"
                                        performanceScore >= 65 -> "Perlu Optimisasi Ringan"
                                        else -> "Beban Sistem Tinggi"
                                    },
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "RAM bebas ${formatGigabytes(ram.availableBytes)} • ${liveFps.toInt()} FPS aktif",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))
                            PerformanceScoreRing(
                                score = performanceScore,
                                label = "SKOR"
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricPill(
                                label = "RAM Bebas",
                                value = formatGigabytes(ram.availableBytes),
                                accent = ElectricCyan,
                                icon = Icons.Rounded.Memory,
                                modifier = Modifier.weight(1f)
                            )
                            MetricPill(
                                label = "Suhu Baterai",
                                value = String.format("%.1f°C", battery.temperatureCelsius),
                                accent = if (battery.temperatureCelsius >= settings.thermalAlertCelsius) CoralCrimson else MintEmerald,
                                icon = Icons.Rounded.Thermostat,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricPill(
                                label = "Refresh Layar",
                                value = "${display.activeRefreshRateHz.toInt()} Hz",
                                accent = MintEmerald,
                                icon = Icons.Rounded.Speed,
                                modifier = Modifier.weight(1f)
                            )
                            MetricPill(
                                label = "Baterai",
                                value = "${battery.levelPercent}%",
                                accent = SolarAmber,
                                icon = Icons.Rounded.BatteryChargingFull,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = onMasterBoost,
                            enabled = !isMasterBoosting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("master_boost_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = ObsidianVoid
                            )
                        ) {
                            if (isMasterBoosting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = ObsidianVoid
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Mengoptimalkan Sistem...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = "Optimalkan Sekarang"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Boost RAM & Bersihkan Sekarang",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Low-Latency AI Quick Pulse Card (gemini-3.1-flash-lite)
        item {
            GlassTelemetryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                borderColor = MintEmerald.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionTitleRow(
                        title = "Diagnosa Kilat AI",
                        subtitle = "Powered by gemini-3.1-flash-lite • Latensi rendah",
                        icon = Icons.Rounded.AutoAwesome,
                        iconTint = MintEmerald,
                        trailing = {
                            OutlinedButton(
                                onClick = onRunQuickAiPulse,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("quick_ai_pulse_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Analisa", color = MintEmerald)
                            }
                        }
                    )

                    when (quickPulseState) {
                        AiResponseState.Idle -> {
                            Text(
                                text = "Tekan \"Analisa\" untuk evaluasi instan RAM, termal baterai, dan kelancaran layar secara real-time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        AiResponseState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MintEmerald
                                )
                                Text(
                                    text = "Menganalisis telemetri perangkat...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                        is AiResponseState.Streaming -> {
                            Text(
                                text = quickPulseState.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                        is AiResponseState.Success -> {
                            Text(
                                text = quickPulseState.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                        is AiResponseState.Error -> {
                            Text(
                                text = quickPulseState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = CoralCrimson
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpenAiAdvisor)
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .testTag("open_deep_ai_link"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Buka Asisten AI Mode Thinking Mendalam",
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricCyan
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Buka Asisten AI",
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Auto Junk Cleaner & Storage Card
        item {
            GlassTelemetryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionTitleRow(
                        title = "Pembersih Sampah Otomatis",
                        subtitle = "Terakhir dibersihkan: ${formatTimestamp(junk.lastCleanTimestampMs)}",
                        icon = Icons.Rounded.CleaningServices,
                        iconTint = ElectricCyan,
                        trailing = {
                            IconButton(
                                onClick = onScanJunk,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("scan_junk_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Pindai Ulang Cache",
                                    tint = ElectricCyan
                                )
                            }
                        }
                    )

                    // Storage overview bar
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Storage,
                                    contentDescription = "Penyimpanan Internal",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Penyimpanan Internal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = "${formatGigabytes(storage.usedBytes)} / ${formatGigabytes(storage.totalBytes)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { storage.usagePercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            color = if (storage.usagePercent > 0.85f) CoralCrimson else ElectricCyan,
                            trackColor = ElevatedSlate
                        )
                        Text(
                            text = "Ruang tersedia: ${formatGigabytes(storage.availableBytes)} • Total dibebaskan: ${formatBytes(junk.totalReclaimedBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }

                    // Auto-Clean schedule selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Schedule,
                                contentDescription = "Jadwal Auto-Clean",
                                tint = MintEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Jadwal Pembersihan Otomatis",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AutoCleanInterval.entries.forEach { interval ->
                                val selected = settings.autoCleanInterval == interval
                                FilterChip(
                                    selected = selected,
                                    onClick = { onSelectAutoCleanInterval(interval) },
                                    label = { Text(interval.label) },
                                    modifier = Modifier.testTag("autoclean_${interval.name.lowercase()}"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanContainer,
                                        selectedLabelColor = ElectricCyan
                                    )
                                )
                            }
                        }
                    }

                    // Scanned Junk Categories
                    AnimatedVisibility(visible = junk.isScanning) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Memindai direktori cache & file residu...", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        junk.categories.forEach { category ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ElevatedSlate.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleJunkCategory(category.id) }
                                    .testTag("junk_category_${category.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = category.selected,
                                        onCheckedChange = { onToggleJunkCategory(category.id) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = ElectricCyan,
                                            checkmarkColor = ObsidianVoid
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = category.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${category.description} (${category.fileCount} file)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatBytes(category.bytes),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (category.bytes > 0L) MintEmerald else TextMuted
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onCleanJunk,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("clean_junk_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MintEmerald,
                                contentColor = ObsidianVoid
                            )
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Bersihkan Cache")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bersihkan (${formatBytes(junk.selectedBytes)})",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                runCatching { context.startActivity(onOpenSystemStorageCleaner()) }
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("system_cache_settings_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cache Sistem", color = ElectricCyan)
                        }
                    }
                }
            }
        }
    }
}
