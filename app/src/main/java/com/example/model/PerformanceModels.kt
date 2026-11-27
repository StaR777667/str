package com.example.model

enum class AutoCleanInterval(val hours: Int, val label: String) {
    OFF(0, "Nonaktif"),
    EVERY_6H(6, "Tiap 6 Jam"),
    DAILY(24, "Harian (24 Jam)"),
    ON_OPEN(1, "Otomatis Saat Buka")
}

enum class GameProfile(val label: String, val subtitle: String, val targetFpsMode: String) {
    BALANCED("Seimbang", "RAM optimal & frame rate stabil", "AUTO"),
    PRO_GAMING("Pro Gaming", "Maksimal Hz, bersihkan RAM agresif", "MAX"),
    COOL_BATTERY("Mode Dingin", "Kunci 60 Hz untuk jaga suhu baterai", "60")
}

enum class AiModelMode(val title: String, val badge: String, val description: String) {
    FAST(
        title = "Kilat (Flash-Lite)",
        badge = "gemini-3.1-flash-lite",
        description = "Respons instan latensi rendah untuk diagnosa & tips cepat"
    ),
    DEEP_THINKING(
        title = "Analisis Mendalam",
        badge = "gemini-4-carbon • Thinking HIGH",
        description = "Penalaran mendalam untuk masalah thermal, lag game, & hardware"
    )
}

data class RamTelemetry(
    val totalBytes: Long = 0L,
    val availableBytes: Long = 0L,
    val cachedBytes: Long = 0L,
    val swapTotalBytes: Long = 0L,
    val swapFreeBytes: Long = 0L,
    val lowMemoryThresholdBytes: Long = 0L,
    val isLowMemory: Boolean = false
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0L)
    val usagePercent: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class StorageTelemetry(
    val totalBytes: Long = 0L,
    val availableBytes: Long = 0L
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0L)
    val usagePercent: Float get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class JunkCategory(
    val id: String,
    val title: String,
    val description: String,
    val bytes: Long,
    val fileCount: Int,
    val selected: Boolean = true
)

data class JunkScanResult(
    val categories: List<JunkCategory> = emptyList(),
    val isScanning: Boolean = false,
    val lastCleanTimestampMs: Long = 0L,
    val lastCleanedBytes: Long = 0L,
    val totalReclaimedBytes: Long = 0L
) {
    val totalJunkBytes: Long get() = categories.sumOf { it.bytes }
    val selectedBytes: Long get() = categories.filter { it.selected }.sumOf { it.bytes }
}

data class DisplayModeOption(
    val modeId: Int,
    val width: Int,
    val height: Int,
    val refreshRateHz: Float
)

data class DisplayTelemetry(
    val activeRefreshRateHz: Float = 60f,
    val activeModeId: Int = -1,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val densityDpi: Int = 0,
    val supportedModes: List<DisplayModeOption> = emptyList(),
    val isHdrSupported: Boolean = false,
    val isWideColorGamut: Boolean = false,
    val glEsVersion: String = "-"
)

data class BatteryTelemetry(
    val temperatureCelsius: Float = 0f,
    val levelPercent: Int = 0,
    val voltageMv: Int = 0,
    val currentMa: Int = 0,
    val powerWatts: Float = 0f,
    val isCharging: Boolean = false,
    val chargeSource: String = "Tidak Mengisi",
    val healthStatus: String = "Baik",
    val technology: String = "Li-ion",
    val chargeCounterMah: Int = 0,
    val thermalStatusLabel: String = "Normal",
    val thermalLevel: Int = 0,
    val historyCelsius: List<Float> = emptyList()
)

data class CpuCoreFreq(
    val coreIndex: Int,
    val currentMhz: Int?,
    val minMhz: Int?,
    val maxMhz: Int?,
    val governor: String?
)

data class SensorSpec(
    val name: String,
    val vendor: String,
    val typeName: String,
    val powerMa: Float,
    val maxRange: Float
)

data class DeviceSpecs(
    val brand: String = "",
    val model: String = "",
    val device: String = "",
    val board: String = "",
    val hardware: String = "",
    val socManufacturer: String = "",
    val socModel: String = "",
    val abis: List<String> = emptyList(),
    val androidVersion: String = "",
    val apiLevel: Int = 0,
    val securityPatch: String = "",
    val buildId: String = "",
    val kernelVersion: String = "",
    val cpuCores: Int = 0,
    val coreFrequencies: List<CpuCoreFreq> = emptyList(),
    val uptimeMinutes: Long = 0L,
    val sensors: List<SensorSpec> = emptyList()
)

data class LaunchableApp(
    val packageName: String,
    val appName: String,
    val isGame: Boolean
)

data class RamBoostResult(
    val freedMb: Int,
    val trimmedAppsCount: Int,
    val beforeAvailMb: Int,
    val afterAvailMb: Int,
    val timestampMs: Long
)

data class OptimizerSettings(
    val autoCleanInterval: AutoCleanInterval = AutoCleanInterval.ON_OPEN,
    val preferredRefreshHz: Float = 0f, // 0f = Auto/Max
    val thermalAlertCelsius: Float = 40f,
    val gameProfile: GameProfile = GameProfile.BALANCED,
    val lastAutoCleanMs: Long = 0L,
    val totalReclaimedBytes: Long = 0L,
    val coolingModeActive: Boolean = false,
    val addedGamePackages: Set<String> = emptySet()
)

sealed interface AiResponseState {
    data object Idle : AiResponseState
    data object Loading : AiResponseState
    data class Streaming(val text: String) : AiResponseState
    data class Success(val text: String, val mode: AiModelMode) : AiResponseState
    data class Error(val message: String) : AiResponseState
}

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val modeUsed: AiModelMode? = null
)
