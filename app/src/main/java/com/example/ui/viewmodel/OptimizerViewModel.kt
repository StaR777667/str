package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.formatBytes
import com.example.core.formatGigabytes
import com.example.data.DeviceTelemetryRepository
import com.example.data.OptimizerPreferences
import com.example.model.AutoCleanInterval
import com.example.model.BatteryTelemetry
import com.example.model.DeviceSpecs
import com.example.model.DisplayTelemetry
import com.example.model.GameProfile
import com.example.model.JunkScanResult
import com.example.model.LaunchableApp
import com.example.model.OptimizerSettings
import com.example.model.RamBoostResult
import com.example.model.RamTelemetry
import com.example.model.StorageTelemetry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OptimizerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DeviceTelemetryRepository(application)
    private val prefs = OptimizerPreferences(application)

    private val _ramTelemetry = MutableStateFlow(RamTelemetry())
    val ramTelemetry: StateFlow<RamTelemetry> = _ramTelemetry.asStateFlow()

    private val _storageTelemetry = MutableStateFlow(StorageTelemetry())
    val storageTelemetry: StateFlow<StorageTelemetry> = _storageTelemetry.asStateFlow()

    private val _junkScanResult = MutableStateFlow(JunkScanResult())
    val junkScanResult: StateFlow<JunkScanResult> = _junkScanResult.asStateFlow()

    private val _displayTelemetry = MutableStateFlow(DisplayTelemetry())
    val displayTelemetry: StateFlow<DisplayTelemetry> = _displayTelemetry.asStateFlow()

    private val _batteryTelemetry = MutableStateFlow(BatteryTelemetry())
    val batteryTelemetry: StateFlow<BatteryTelemetry> = _batteryTelemetry.asStateFlow()

    private val _deviceSpecs = MutableStateFlow(DeviceSpecs())
    val deviceSpecs: StateFlow<DeviceSpecs> = _deviceSpecs.asStateFlow()

    private val _launchableApps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val launchableApps: StateFlow<List<LaunchableApp>> = _launchableApps.asStateFlow()

    private val _settings = MutableStateFlow(OptimizerSettings())
    val settings: StateFlow<OptimizerSettings> = _settings.asStateFlow()

    private val _lastRamBoostResult = MutableStateFlow<RamBoostResult?>(null)
    val lastRamBoostResult: StateFlow<RamBoostResult?> = _lastRamBoostResult.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isMasterBoosting = MutableStateFlow(false)
    val isMasterBoosting: StateFlow<Boolean> = _isMasterBoosting.asStateFlow()

    private val _liveFps = MutableStateFlow(60f)
    val liveFps: StateFlow<Float> = _liveFps.asStateFlow()

    private val _liveFrameTimeMs = MutableStateFlow(16.6f)
    val liveFrameTimeMs: StateFlow<Float> = _liveFrameTimeMs.asStateFlow()

    private val _performanceScore = MutableStateFlow(85)
    val performanceScore: StateFlow<Int> = _performanceScore.asStateFlow()

    private val thermalHistory = ArrayDeque<Float>(40)

    init {
        observePreferences()
        observeBattery()
        startTelemetryPolling()
        loadInitialDataAndAutoClean()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            prefs.settingsFlow.collect { saved ->
                _settings.value = saved
                _junkScanResult.update {
                    it.copy(
                        lastCleanTimestampMs = saved.lastAutoCleanMs,
                        totalReclaimedBytes = saved.totalReclaimedBytes
                    )
                }
            }
        }
    }

    private fun observeBattery() {
        viewModelScope.launch {
            repo.observeBatteryTelemetry().collect { battery ->
                if (battery.temperatureCelsius > 0f) {
                    if (thermalHistory.size >= 40) thermalHistory.removeFirst()
                    thermalHistory.addLast(battery.temperatureCelsius)
                }
                _batteryTelemetry.value = battery.copy(historyCelsius = thermalHistory.toList())
                recomputePerformanceScore()
            }
        }
    }

    private fun startTelemetryPolling() {
        viewModelScope.launch {
            while (isActive) {
                _ramTelemetry.value = repo.readRamTelemetry()
                _storageTelemetry.value = repo.readStorageTelemetry()
                _displayTelemetry.value = repo.readDisplayTelemetry()

                // Append a thermal sample periodically so the real-time graph advances steadily
                val latestTemp = _batteryTelemetry.value.temperatureCelsius
                if (latestTemp > 0f) {
                    if (thermalHistory.size >= 40) thermalHistory.removeFirst()
                    thermalHistory.addLast(latestTemp)
                    _batteryTelemetry.update { it.copy(historyCelsius = thermalHistory.toList()) }
                }

                // Refresh live CPU per-core frequencies
                if (_deviceSpecs.value.cpuCores > 0) {
                    val freshFreqs = repo.readCpuCoreFrequencies(_deviceSpecs.value.cpuCores)
                    _deviceSpecs.update { it.copy(coreFrequencies = freshFreqs) }
                }

                recomputePerformanceScore()
                delay(2000L)
            }
        }
    }

    private fun loadInitialDataAndAutoClean() {
        viewModelScope.launch {
            _deviceSpecs.value = repo.readDeviceSpecs()
            _launchableApps.value = repo.queryLaunchableApps()
            scanJunkInternal()

            val currentSettings = prefs.settingsFlow.first()
            val now = System.currentTimeMillis()
            val shouldAutoClean = when (currentSettings.autoCleanInterval) {
                AutoCleanInterval.OFF -> false
                AutoCleanInterval.ON_OPEN -> true
                AutoCleanInterval.EVERY_6H -> (now - currentSettings.lastAutoCleanMs) >= 6L * 3600_000L
                AutoCleanInterval.DAILY -> (now - currentSettings.lastAutoCleanMs) >= 24L * 3600_000L
            }

            if (shouldAutoClean && _junkScanResult.value.totalJunkBytes > 0L) {
                val freed = cleanSelectedJunkInternal()
                if (freed > 0L) {
                    _statusMessage.value = "Auto-Clean membersihkan ${formatBytes(freed)} cache lokal"
                }
            }
        }
    }

    fun updateLiveFps(fps: Float, frameTimeMs: Float) {
        _liveFps.value = fps
        _liveFrameTimeMs.value = frameTimeMs
    }

    fun scanJunk() {
        viewModelScope.launch {
            scanJunkInternal()
        }
    }

    private suspend fun scanJunkInternal() {
        _junkScanResult.update { it.copy(isScanning = true) }
        val categories = repo.scanJunkCategories()
        _junkScanResult.update {
            it.copy(
                categories = categories,
                isScanning = false
            )
        }
        recomputePerformanceScore()
    }

    fun toggleJunkCategory(id: String) {
        _junkScanResult.update { current ->
            current.copy(
                categories = current.categories.map { cat ->
                    if (cat.id == id) cat.copy(selected = !cat.selected) else cat
                }
            )
        }
    }

    fun cleanJunk() {
        viewModelScope.launch {
            val freed = cleanSelectedJunkInternal()
            _statusMessage.value = if (freed > 0L) {
                "Berhasil membersihkan ${formatBytes(freed)} file sampah!"
            } else {
                "Penyimpanan cache aplikasi sudah bersih."
            }
        }
    }

    private suspend fun cleanSelectedJunkInternal(): Long {
        val selectedIds = _junkScanResult.value.categories
            .filter { it.selected }
            .map { it.id }
            .toSet()
        if (selectedIds.isEmpty()) return 0L

        _junkScanResult.update { it.copy(isScanning = true) }
        val freed = repo.cleanSelectedJunk(selectedIds)
        val now = System.currentTimeMillis()
        prefs.recordCleanSweep(freed, now)

        val updatedCategories = repo.scanJunkCategories()
        _junkScanResult.update {
            it.copy(
                categories = updatedCategories,
                isScanning = false,
                lastCleanTimestampMs = now,
                lastCleanedBytes = freed,
                totalReclaimedBytes = it.totalReclaimedBytes + freed
            )
        }
        _storageTelemetry.value = repo.readStorageTelemetry()
        recomputePerformanceScore()
        return freed
    }

    fun boostAll() {
        viewModelScope.launch {
            if (_isMasterBoosting.value) return@launch
            _isMasterBoosting.value = true
            val freedJunk = cleanSelectedJunkInternal()
            val ramResult = repo.boostRamAndTrimBackground()
            _lastRamBoostResult.value = ramResult
            _ramTelemetry.value = repo.readRamTelemetry()
            recomputePerformanceScore()
            _isMasterBoosting.value = false
            _statusMessage.value =
                "Optimisasi selesai: ${ramResult.freedMb} MB RAM dibebaskan • ${formatBytes(freedJunk)} cache dibersihkan"
        }
    }

    fun boostRam() {
        viewModelScope.launch {
            val result = repo.boostRamAndTrimBackground()
            _lastRamBoostResult.value = result
            _ramTelemetry.value = repo.readRamTelemetry()
            recomputePerformanceScore()
            _statusMessage.value =
                "RAM dioptimalkan: ${result.trimmedAppsCount} aplikasi latar dipangkas (+${result.freedMb} MB bebas)"
        }
    }

    fun setAutoCleanInterval(interval: AutoCleanInterval) {
        viewModelScope.launch {
            prefs.setAutoCleanInterval(interval)
            _statusMessage.value = "Jadwal Auto-Clean diubah ke: ${interval.label}"
        }
    }

    fun selectRefreshRate(hz: Float) {
        viewModelScope.launch {
            prefs.setPreferredRefreshHz(hz)
            if (hz > 60f && _settings.value.coolingModeActive) {
                prefs.setCoolingModeActive(false)
            }
            _statusMessage.value = if (hz <= 0f) {
                "Refresh rate diatur ke Otomatis (Maksimal Perangkat)"
            } else {
                "Target kelancaran layar diatur ke ${hz.toInt()} Hz"
            }
        }
    }

    fun selectGameProfile(profile: GameProfile) {
        viewModelScope.launch {
            prefs.setGameProfile(profile)
            when (profile) {
                GameProfile.PRO_GAMING -> {
                    val maxHz = _displayTelemetry.value.supportedModes.maxOfOrNull { it.refreshRateHz } ?: 0f
                    prefs.setPreferredRefreshHz(maxHz)
                    prefs.setCoolingModeActive(false)
                    boostRam()
                }
                GameProfile.COOL_BATTERY -> {
                    prefs.setPreferredRefreshHz(60f)
                    prefs.setCoolingModeActive(true)
                }
                GameProfile.BALANCED -> {
                    prefs.setPreferredRefreshHz(0f)
                    prefs.setCoolingModeActive(false)
                }
            }
            _statusMessage.value = "Profil ${profile.label} diaktifkan"
        }
    }

    fun setThermalAlertCelsius(tempC: Float) {
        viewModelScope.launch {
            prefs.setThermalAlertCelsius(tempC)
        }
    }

    fun toggleCoolingMode() {
        viewModelScope.launch {
            val next = !_settings.value.coolingModeActive
            prefs.setCoolingModeActive(next)
            if (next) {
                prefs.setPreferredRefreshHz(60f)
                repo.boostRamAndTrimBackground()
                _ramTelemetry.value = repo.readRamTelemetry()
                _statusMessage.value = "Mode Pendingin Aktif: Layar 60 Hz & proses latar dipangkas"
            } else {
                prefs.setPreferredRefreshHz(0f)
                _statusMessage.value = "Mode Pendingin dinonaktifkan"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun refreshLaunchableApps() {
        viewModelScope.launch {
            _launchableApps.value = repo.queryLaunchableApps()
        }
    }

    fun toggleAddedGame(app: LaunchableApp) {
        viewModelScope.launch {
            val added = prefs.toggleGamePackage(app.packageName)
            _statusMessage.value = if (added) {
                "${app.appName} ditambahkan ke daftar optimisasi game"
            } else {
                "${app.appName} dihapus dari daftar optimisasi game"
            }
        }
    }

    fun removeAddedGame(app: LaunchableApp) {
        viewModelScope.launch {
            prefs.removeGamePackage(app.packageName)
            _statusMessage.value = "${app.appName} dihapus dari daftar game"
        }
    }

    fun boostForGame(app: LaunchableApp) {
        viewModelScope.launch {
            val freedCache = cleanSelectedJunkInternal()
            val ramResult = repo.boostRamAndTrimBackground()
            _lastRamBoostResult.value = ramResult
            _ramTelemetry.value = repo.readRamTelemetry()
            recomputePerformanceScore()
            _statusMessage.value =
                "Optimisasi untuk ${app.appName}: +${ramResult.freedMb} MB RAM & ${formatBytes(freedCache)} cache dibersihkan"
        }
    }

    fun getSystemStorageClearIntent(): Intent = repo.buildSystemStorageClearIntent()

    fun getDisplaySettingsIntent(): Intent = repo.buildDisplaySettingsIntent()

    fun buildTelemetrySummaryForAi(): String {
        val ram = _ramTelemetry.value
        val storage = _storageTelemetry.value
        val battery = _batteryTelemetry.value
        val display = _displayTelemetry.value
        val specs = _deviceSpecs.value
        return buildString {
            appendLine("Perangkat: ${specs.brand} ${specs.model} (${specs.device})")
            appendLine("SoC / Hardware: ${specs.socManufacturer} ${specs.socModel} | ${specs.cpuCores} Core CPU")
            appendLine("Android: ${specs.androidVersion} (API ${specs.apiLevel})")
            appendLine("RAM: Terpakai ${formatGigabytes(ram.usedBytes)} / ${formatGigabytes(ram.totalBytes)} (Bebas: ${formatGigabytes(ram.availableBytes)})")
            appendLine("Penyimpanan: Terpakai ${formatGigabytes(storage.usedBytes)} / ${formatGigabytes(storage.totalBytes)} (Bebas: ${formatGigabytes(storage.availableBytes)})")
            appendLine("Layar: ${display.widthPx}x${display.heightPx}, Refresh Rate Aktif ${display.activeRefreshRateHz.toInt()} Hz, Live FPS ${_liveFps.value.toInt()} FPS")
            appendLine("Baterai & Termal: Suhu ${String.format("%.1f", battery.temperatureCelsius)}°C, Level ${battery.levelPercent}%, Status ${battery.chargeSource}, Termal OS: ${battery.thermalStatusLabel}")
            appendLine("Cache Terdeteksi: ${formatBytes(_junkScanResult.value.totalJunkBytes)}")
        }
    }

    private fun recomputePerformanceScore() {
        val ram = _ramTelemetry.value
        val battery = _batteryTelemetry.value
        val junkBytes = _junkScanResult.value.totalJunkBytes
        val display = _displayTelemetry.value

        var score = 100
        // RAM pressure penalty
        val ramUsage = ram.usagePercent
        if (ramUsage > 0.85f) score -= 25
        else if (ramUsage > 0.70f) score -= 14
        else if (ramUsage > 0.55f) score -= 6

        // Thermal penalty
        val temp = battery.temperatureCelsius
        if (temp >= 43f) score -= 25
        else if (temp >= 39f) score -= 14
        else if (temp >= 36f) score -= 5

        // Cache residue penalty
        if (junkBytes > 50L * 1024L * 1024L) score -= 12
        else if (junkBytes > 10L * 1024L * 1024L) score -= 6

        // Bonus for high refresh rate support active
        if (display.activeRefreshRateHz >= 90f) score += 2

        _performanceScore.value = score.coerceIn(35, 100)
    }
}
