package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ScreenshotMonitor
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.formatGigabytes
import com.example.model.DeviceSpecs
import com.example.model.DisplayTelemetry
import com.example.model.RamTelemetry
import com.example.model.StorageTelemetry
import com.example.ui.components.GlassTelemetryCard
import com.example.ui.components.SectionTitleRow
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private enum class SpecCategory(val label: String) {
    ALL("Semua"),
    CPU("CPU & SoC"),
    MEMORY("Memori"),
    DISPLAY("Layar & GPU"),
    SYSTEM("Sistem"),
    SENSORS("Sensor")
}

@Composable
fun SpecsScreen(
    specs: DeviceSpecs,
    ram: RamTelemetry,
    storage: StorageTelemetry,
    display: DisplayTelemetry,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(SpecCategory.ALL) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.label) },
                        modifier = Modifier.testTag("spec_filter_${cat.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanContainer,
                            selectedLabelColor = ElectricCyan
                        )
                    )
                }
            }
        }

        // CPU & SoC
        if (selectedCategory == SpecCategory.ALL || selectedCategory == SpecCategory.CPU) {
            item {
                GlassTelemetryCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SectionTitleRow(
                            title = "Prosesor & Arsitektur SoC",
                            subtitle = "${specs.socManufacturer} ${specs.socModel}",
                            icon = Icons.Rounded.DeveloperBoard,
                            iconTint = ElectricCyan
                        )
                        SpecRow("Chipset / SoC", "${specs.socManufacturer} ${specs.socModel}")
                        SpecRow("Hardware / Board", "${specs.hardware} (${specs.board})")
                        SpecRow("Jumlah Core CPU", "${specs.cpuCores} Core")
                        SpecRow("Arsitektur ABI", specs.abis.joinToString(", ").ifBlank { "-" })

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Frekuensi Real-Time Per Core",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        specs.coreFrequencies.forEach { core ->
                            val maxMhz = (core.maxMhz ?: 2400).coerceAtLeast(1000)
                            val curMhz = core.currentMhz
                            val ratio = if (curMhz != null) (curMhz.toFloat() / maxMhz.toFloat()).coerceIn(0.05f, 1f) else 0f
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Core ${core.coreIndex} ${core.governor?.let { "($it)" } ?: ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = if (curMhz != null) "$curMhz / ${core.maxMhz ?: "?"} MHz" else "Dikelola Kernel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (curMhz != null) MintEmerald else TextMuted
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(999.dp)),
                                    color = ElectricCyan,
                                    trackColor = ElevatedSlate
                                )
                            }
                        }
                    }
                }
            }
        }

        // Memory & Storage
        if (selectedCategory == SpecCategory.ALL || selectedCategory == SpecCategory.MEMORY) {
            item {
                GlassTelemetryCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionTitleRow(
                            title = "Memori RAM & Penyimpanan",
                            subtitle = "Total RAM ${formatGigabytes(ram.totalBytes)}",
                            icon = Icons.Rounded.Memory,
                            iconTint = MintEmerald
                        )
                        SpecRow("Total RAM Fisik", formatGigabytes(ram.totalBytes))
                        SpecRow("RAM Tersedia", formatGigabytes(ram.availableBytes))
                        SpecRow("Cache & Buffer Kernel", formatGigabytes(ram.cachedBytes))
                        SpecRow(
                            "Swap / ZRAM",
                            if (ram.swapTotalBytes > 0L) {
                                "${formatGigabytes(ram.swapTotalBytes - ram.swapFreeBytes)} / ${formatGigabytes(ram.swapTotalBytes)}"
                            } else {
                                "Tidak aktif"
                            }
                        )
                        SpecRow("Ambang Batas Low-Memory", formatGigabytes(ram.lowMemoryThresholdBytes))
                        SpecRow("Penyimpanan Total", formatGigabytes(storage.totalBytes))
                        SpecRow("Penyimpanan Tersisa", formatGigabytes(storage.availableBytes))
                    }
                }
            }
        }

        // Display & GPU
        if (selectedCategory == SpecCategory.ALL || selectedCategory == SpecCategory.DISPLAY) {
            item {
                GlassTelemetryCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionTitleRow(
                            title = "Layar & Subsistem Grafis",
                            subtitle = "${display.widthPx} × ${display.heightPx} px • OpenGL ES ${display.glEsVersion}",
                            icon = Icons.Rounded.ScreenshotMonitor,
                            iconTint = SolarAmber
                        )
                        SpecRow("Resolusi Fisik", "${display.widthPx} × ${display.heightPx} px")
                        SpecRow("Kepadatan Layar", "${display.densityDpi} dpi")
                        SpecRow("Refresh Rate Aktif", "${display.activeRefreshRateHz.toInt()} Hz")
                        SpecRow(
                            "Mode Didukung",
                            display.supportedModes
                                .map { "${it.refreshRateHz.toInt()} Hz" }
                                .distinct()
                                .joinToString(", ")
                                .ifBlank { "60 Hz" }
                        )
                        SpecRow("OpenGL ES", display.glEsVersion)
                        SpecRow("Dukungan HDR", if (display.isHdrSupported) "Ya (Didukung)" else "Standar SDR")
                        SpecRow("Wide Color Gamut", if (display.isWideColorGamut) "Ya (DCI-P3 / Wide)" else "sRGB")
                    }
                }
            }
        }

        // System & OS
        if (selectedCategory == SpecCategory.ALL || selectedCategory == SpecCategory.SYSTEM) {
            item {
                GlassTelemetryCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionTitleRow(
                            title = "Identitas Perangkat & OS",
                            subtitle = "${specs.brand} ${specs.model}",
                            icon = Icons.Rounded.PhoneAndroid,
                            iconTint = ElectricCyan
                        )
                        SpecRow("Merek & Model", "${specs.brand} ${specs.model}")
                        SpecRow("Codename Perangkat", specs.device)
                        SpecRow("Versi Android", "Android ${specs.androidVersion} (API ${specs.apiLevel})")
                        SpecRow("Patch Keamanan", specs.securityPatch.ifBlank { "-" })
                        SpecRow("Build ID", specs.buildId)
                        SpecRow("Versi Kernel", specs.kernelVersion)
                        SpecRow("Uptime Sistem", "${specs.uptimeMinutes} menit")
                    }
                }
            }
        }

        // Hardware Sensors
        if (selectedCategory == SpecCategory.ALL || selectedCategory == SpecCategory.SENSORS) {
            item {
                GlassTelemetryCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SectionTitleRow(
                            title = "Daftar Sensor Perangkat",
                            subtitle = "${specs.sensors.size} sensor hardware terdeteksi",
                            icon = Icons.Rounded.Sensors,
                            iconTint = MintEmerald
                        )
                    }
                }
            }

            items(specs.sensors, key = { "${it.name}_${it.typeName}_${it.vendor}" }) { sensor ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = ElevatedSlate.copy(alpha = 0.75f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = sensor.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "${sensor.typeName} • ${sensor.vendor} • ${String.format("%.2f", sensor.powerMa)} mA",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.weight(0.44f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            modifier = Modifier.weight(0.56f)
        )
    }
}
