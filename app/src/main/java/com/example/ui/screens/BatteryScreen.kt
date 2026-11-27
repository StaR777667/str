package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BatteryTelemetry
import com.example.model.OptimizerSettings
import com.example.ui.components.GlassTelemetryCard
import com.example.ui.components.SectionTitleRow
import com.example.ui.components.ThermalArcGauge
import com.example.ui.components.ThermalHistoryChart
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun BatteryScreen(
    battery: BatteryTelemetry,
    settings: OptimizerSettings,
    onSetThermalAlert: (Float) -> Unit,
    onToggleCoolingMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOverheating = battery.temperatureCelsius >= settings.thermalAlertCelsius

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            AnimatedVisibility(visible = isOverheating) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = CrimsonContainer,
                    border = BorderStroke(1.dp, CoralCrimson)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = "Peringatan Suhu Tinggi",
                            tint = CoralCrimson
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Suhu baterai mencapai ${String.format("%.1f°C", battery.temperatureCelsius)}, melewati batas alert ${settings.thermalAlertCelsius.roundToInt()}°C!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Live Thermal Arc Gauge Card
        item {
            GlassTelemetryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
                borderColor = if (isOverheating) CoralCrimson.copy(alpha = 0.6f) else ElectricCyan.copy(alpha = 0.35f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SectionTitleRow(
                        title = "Pemantau Suhu Real-Time",
                        subtitle = "Status Termal OS: ${battery.thermalStatusLabel}",
                        icon = Icons.Rounded.Thermostat,
                        iconTint = if (isOverheating) CoralCrimson else ElectricCyan
                    )

                    ThermalArcGauge(
                        temperatureCelsius = battery.temperatureCelsius,
                        alertThresholdCelsius = settings.thermalAlertCelsius
                    )

                    Button(
                        onClick = onToggleCoolingMode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("toggle_cooling_mode_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (settings.coolingModeActive) MintEmerald else ElectricCyan,
                            contentColor = ObsidianVoid
                        )
                    ) {
                        Icon(
                            Icons.Rounded.AcUnit,
                            contentDescription = "Mode Pendingin Cepat"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (settings.coolingModeActive) {
                                "Mode Pendingin Aktif (60 Hz + Hemat Daya)"
                            } else {
                                "Aktifkan Mode Pendingin Cepat"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Real-Time Thermal Trend Chart
        item {
            GlassTelemetryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionTitleRow(
                        title = "Grafik Tren Suhu Live",
                        subtitle = "${battery.historyCelsius.size} sampel pembacaan sensor baterai",
                        icon = Icons.Rounded.ShowChart,
                        iconTint = MintEmerald
                    )

                    ThermalHistoryChart(
                        history = battery.historyCelsius,
                        thresholdCelsius = settings.thermalAlertCelsius
                    )

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Batas Peringatan Suhu",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = "${settings.thermalAlertCelsius.roundToInt()}°C",
                                style = MaterialTheme.typography.labelLarge,
                                color = SolarAmber
                            )
                        }
                        Slider(
                            value = settings.thermalAlertCelsius,
                            onValueChange = onSetThermalAlert,
                            valueRange = 36f..46f,
                            steps = 9,
                            modifier = Modifier.testTag("thermal_threshold_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = SolarAmber,
                                activeTrackColor = SolarAmber
                            )
                        )
                    }
                }
            }
        }

        // Deep Battery Telemetry Grid
        item {
            GlassTelemetryCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionTitleRow(
                        title = "Telemetri Listrik & Kesehatan",
                        subtitle = "${battery.chargeSource} • ${battery.technology}",
                        icon = Icons.Rounded.BatteryChargingFull,
                        iconTint = SolarAmber
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BatteryStatBox(
                            label = "Kapasitas Tersisa",
                            value = "${battery.levelPercent}%",
                            accent = MintEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        BatteryStatBox(
                            label = "Tegangan",
                            value = "${battery.voltageMv} mV",
                            accent = ElectricCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BatteryStatBox(
                            label = "Arus Listrik",
                            value = "${battery.currentMa} mA",
                            accent = SolarAmber,
                            modifier = Modifier.weight(1f)
                        )
                        BatteryStatBox(
                            label = "Daya Seketika",
                            value = String.format("%.2f W", battery.powerWatts),
                            accent = ElectricCyan,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BatteryStatBox(
                            label = "Kesehatan",
                            value = battery.healthStatus,
                            accent = MintEmerald,
                            modifier = Modifier.weight(1f)
                        )
                        BatteryStatBox(
                            label = "Muatan Terukur",
                            value = if (battery.chargeCounterMah > 0) "${battery.chargeCounterMah} mAh" else "N/A",
                            accent = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryStatBox(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = ElevatedSlate
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.ElectricBolt,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.height(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = accent
            )
        }
    }
}
