package com.example.data

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import androidx.core.content.ContextCompat
import com.example.model.BatteryTelemetry
import com.example.model.CpuCoreFreq
import com.example.model.DeviceSpecs
import com.example.model.DisplayModeOption
import com.example.model.DisplayTelemetry
import com.example.model.JunkCategory
import com.example.model.LaunchableApp
import com.example.model.RamBoostResult
import com.example.model.RamTelemetry
import com.example.model.SensorSpec
import com.example.model.StorageTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

class DeviceTelemetryRepository(private val context: Context) {

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val batteryManager =
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val powerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val displayManager =
        context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    suspend fun readRamTelemetry(): RamTelemetry = withContext(Dispatchers.IO) {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val procValues = readProcMemInfo()
        val total = procValues["MemTotal"] ?: memInfo.totalMem
        val avail = memInfo.availMem.takeIf { it > 0 } ?: (procValues["MemAvailable"] ?: 0L)
        val cached = (procValues["Cached"] ?: 0L) + (procValues["Buffers"] ?: 0L)
        val swapTotal = procValues["SwapTotal"] ?: 0L
        val swapFree = procValues["SwapFree"] ?: 0L

        RamTelemetry(
            totalBytes = total,
            availableBytes = avail,
            cachedBytes = cached,
            swapTotalBytes = swapTotal,
            swapFreeBytes = swapFree,
            lowMemoryThresholdBytes = memInfo.threshold,
            isLowMemory = memInfo.lowMemory
        )
    }

    private fun readProcMemInfo(): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        runCatching {
            File("/proc/meminfo").forEachLine { line ->
                val parts = line.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val valueKb = parts[1].trim().split(Regex("\\s+")).firstOrNull()?.toLongOrNull()
                    if (valueKb != null) {
                        result[key] = valueKb * 1024L
                    }
                }
            }
        }
        return result
    }

    suspend fun readStorageTelemetry(): StorageTelemetry = withContext(Dispatchers.IO) {
        val statFs = StatFs(Environment.getDataDirectory().path)
        StorageTelemetry(
            totalBytes = statFs.totalBytes,
            availableBytes = statFs.availableBytes
        )
    }

    suspend fun scanJunkCategories(): List<JunkCategory> = withContext(Dispatchers.IO) {
        val internalCache = dirSizeAndCount(context.cacheDir)
        val externalCache = context.externalCacheDir?.let { dirSizeAndCount(it) } ?: (0L to 0)
        val codeCache = dirSizeAndCount(context.codeCacheDir)
        val tempAndLogs = scanTempFiles(context.filesDir)

        listOf(
            JunkCategory(
                id = "app_cache",
                title = "Cache Aplikasi Internal",
                description = "Cache runtime, webview, dan buffer sementara",
                bytes = internalCache.first,
                fileCount = internalCache.second,
                selected = true
            ),
            JunkCategory(
                id = "ext_cache",
                title = "Cache Media & Eksternal",
                description = "Thumbnail, aset sementara, & cache eksternal",
                bytes = externalCache.first,
                fileCount = externalCache.second,
                selected = true
            ),
            JunkCategory(
                id = "code_cache",
                title = "Cache Kode & Shader",
                description = "Kompilasi bytecode & shader grafis lama",
                bytes = codeCache.first,
                fileCount = codeCache.second,
                selected = true
            ),
            JunkCategory(
                id = "temp_logs",
                title = "File Log & Residu Temp",
                description = "File .tmp, .log, & sisa diagnostik",
                bytes = tempAndLogs.first,
                fileCount = tempAndLogs.second,
                selected = true
            )
        )
    }

    suspend fun cleanSelectedJunk(selectedIds: Set<String>): Long = withContext(Dispatchers.IO) {
        var reclaimed = 0L
        if ("app_cache" in selectedIds) {
            reclaimed += clearDirectoryContents(context.cacheDir)
        }
        if ("ext_cache" in selectedIds) {
            context.externalCacheDir?.let { reclaimed += clearDirectoryContents(it) }
        }
        if ("code_cache" in selectedIds) {
            reclaimed += clearDirectoryContents(context.codeCacheDir)
        }
        if ("temp_logs" in selectedIds) {
            reclaimed += deleteTempAndLogs(context.filesDir)
        }
        reclaimed
    }

    private fun dirSizeAndCount(dir: File?): Pair<Long, Int> {
        if (dir == null || !dir.exists()) return 0L to 0
        var size = 0L
        var count = 0
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
                count++
            }
        }
        return size to count
    }

    private fun scanTempFiles(dir: File?): Pair<Long, Int> {
        if (dir == null || !dir.exists()) return 0L to 0
        var size = 0L
        var count = 0
        dir.walkTopDown().forEach { file ->
            if (file.isFile && isTempOrLogFile(file)) {
                size += file.length()
                count++
            }
        }
        return size to count
    }

    private fun clearDirectoryContents(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var freed = 0L
        dir.listFiles()?.forEach { child ->
            freed += deleteRecursivelyWithBytes(child)
        }
        return freed
    }

    private fun deleteTempAndLogs(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var freed = 0L
        dir.walkBottomUp().forEach { file ->
            if (file.isFile && isTempOrLogFile(file)) {
                val len = file.length()
                if (file.delete()) {
                    freed += len
                }
            }
        }
        return freed
    }

    private fun isTempOrLogFile(file: File): Boolean {
        val name = file.name.lowercase()
        val parent = file.parentFile?.name?.lowercase() ?: ""
        return name.endsWith(".tmp") ||
            name.endsWith(".log") ||
            name.endsWith(".bak") ||
            parent == "tmp" ||
            parent == "logs"
    }

    private fun deleteRecursivelyWithBytes(file: File): Long {
        var bytes = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach { bytes += deleteRecursivelyWithBytes(it) }
            file.delete()
        } else {
            val len = file.length()
            if (file.delete()) {
                bytes += len
            }
        }
        return bytes
    }

    fun buildSystemStorageClearIntent(): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val clearIntent = Intent("android.os.storage.action.CLEAR_APP_CACHE")
            if (clearIntent.resolveActivity(context.packageManager) != null) {
                return clearIntent
            }
        }
        val storageIntent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        return if (storageIntent.resolveActivity(context.packageManager) != null) {
            storageIntent
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    fun buildDisplaySettingsIntent(): Intent {
        val displayIntent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
        return if (displayIntent.resolveActivity(context.packageManager) != null) {
            displayIntent
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
    }

    suspend fun boostRamAndTrimBackground(): RamBoostResult = withContext(Dispatchers.IO) {
        val beforeInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(beforeInfo)
        val beforeAvailMb = (beforeInfo.availMem / (1024L * 1024L)).toInt()

        val launchables = queryLaunchableAppsInternal()
        var trimmedCount = 0
        launchables.forEach { app ->
            if (app.packageName != context.packageName) {
                runCatching {
                    activityManager.killBackgroundProcesses(app.packageName)
                    trimmedCount++
                }
            }
        }

        // Encourage JVM heap compaction for our own process memory footprint
        Runtime.getRuntime().gc()

        val afterInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(afterInfo)
        val afterAvailMb = (afterInfo.availMem / (1024L * 1024L)).toInt()
        val freedMb = (afterAvailMb - beforeAvailMb).coerceAtLeast(0)

        RamBoostResult(
            freedMb = freedMb,
            trimmedAppsCount = trimmedCount,
            beforeAvailMb = beforeAvailMb,
            afterAvailMb = afterAvailMb,
            timestampMs = System.currentTimeMillis()
        )
    }

    suspend fun queryLaunchableApps(): List<LaunchableApp> = withContext(Dispatchers.IO) {
        queryLaunchableAppsInternal()
    }

    private fun queryLaunchableAppsInternal(): List<LaunchableApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        return resolveInfos
            .asSequence()
            .mapNotNull { info ->
                val appInfo = info.activityInfo?.applicationInfo ?: return@mapNotNull null
                val pkg = appInfo.packageName
                if (pkg == context.packageName) return@mapNotNull null

                val isGame = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appInfo.category == ApplicationInfo.CATEGORY_GAME ||
                        @Suppress("DEPRECATION")
                        ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0)
                } else {
                    @Suppress("DEPRECATION")
                    ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0)
                }

                LaunchableApp(
                    packageName = pkg,
                    appName = info.loadLabel(pm).toString(),
                    isGame = isGame
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(compareByDescending<LaunchableApp> { it.isGame }.thenBy { it.appName.lowercase() })
            .toList()
    }

    fun readDisplayTelemetry(): DisplayTelemetry {
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
        val activeMode = display?.mode
        val supportedModes = display?.supportedModes?.map { mode ->
            DisplayModeOption(
                modeId = mode.modeId,
                width = mode.physicalWidth,
                height = mode.physicalHeight,
                refreshRateHz = mode.refreshRate
            )
        }?.sortedByDescending { it.refreshRateHz } ?: emptyList()

        val metrics: DisplayMetrics = context.resources.displayMetrics
        val hdrSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            (display?.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() == true)
        } else {
            false
        }
        val wideGamut = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            display?.isWideColorGamut == true
        } else {
            false
        }

        return DisplayTelemetry(
            activeRefreshRateHz = activeMode?.refreshRate ?: display?.refreshRate ?: 60f,
            activeModeId = activeMode?.modeId ?: -1,
            widthPx = activeMode?.physicalWidth ?: metrics.widthPixels,
            heightPx = activeMode?.physicalHeight ?: metrics.heightPixels,
            densityDpi = metrics.densityDpi,
            supportedModes = supportedModes,
            isHdrSupported = hdrSupported,
            isWideColorGamut = wideGamut,
            glEsVersion = activityManager.deviceConfigurationInfo.glEsVersion
        )
    }

    fun observeBatteryTelemetry(): Flow<BatteryTelemetry> = callbackFlow {
        fun buildTelemetry(intent: Intent?): BatteryTelemetry {
            val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempC = tempTenths / 10f
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val levelPct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else 0
            val voltageMv = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
            val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
                ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
            val tech = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.ifBlank { "Li-ion" } ?: "Li-ion"

            val rawCurrent = runCatching {
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            }.getOrDefault(0)
            // Normalize microamps vs milliamps across OEM implementations
            val currentMa = if (abs(rawCurrent) > 10_000) rawCurrent / 1000 else rawCurrent

            val chargeCounterRaw = runCatching {
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            }.getOrDefault(0)
            val chargeCounterMah = if (chargeCounterRaw > 100_000) chargeCounterRaw / 1000 else chargeCounterRaw

            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

            val chargeSource = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "Adaptor AC"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Nirkabel"
                else -> if (status == BatteryManager.BATTERY_STATUS_FULL) "Penuh" else "Baterai (Tidak Mengisi)"
            }

            val healthLabel = when (health) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Baik (Sehat)"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Terlalu Panas"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Rusak"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Tegangan Berlebih"
                BatteryManager.BATTERY_HEALTH_COLD -> "Terlalu Dingin"
                else -> "Normal"
            }

            val (thermalLevel, thermalLabel) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val tStatus = powerManager.currentThermalStatus
                val label = when (tStatus) {
                    PowerManager.THERMAL_STATUS_NONE -> "Optimal (Tidak Ada Throttling)"
                    PowerManager.THERMAL_STATUS_LIGHT -> "Hangat Ringan"
                    PowerManager.THERMAL_STATUS_MODERATE -> "Moderat (Pantau Suhu)"
                    PowerManager.THERMAL_STATUS_SEVERE -> "Tinggi (Throttling Aktif)"
                    PowerManager.THERMAL_STATUS_CRITICAL -> "Kritis (Segera Dinginkan)"
                    PowerManager.THERMAL_STATUS_EMERGENCY,
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "Darurat Thermal"
                    else -> "Normal"
                }
                tStatus to label
            } else {
                val label = when {
                    tempC >= 43f -> "Panas Tinggi"
                    tempC >= 38f -> "Hangat"
                    else -> "Optimal"
                }
                0 to label
            }

            val watts = (abs(currentMa) * (voltageMv / 1000f)) / 1000f

            return BatteryTelemetry(
                temperatureCelsius = tempC,
                levelPercent = levelPct,
                voltageMv = voltageMv,
                currentMa = currentMa,
                powerWatts = watts,
                isCharging = isCharging,
                chargeSource = chargeSource,
                healthStatus = healthLabel,
                technology = tech,
                chargeCounterMah = chargeCounterMah,
                thermalStatusLabel = thermalLabel,
                thermalLevel = thermalLevel
            )
        }

        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val initialIntent = ContextCompat.registerReceiver(
            context,
            null,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        trySend(buildTelemetry(initialIntent))

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                trySend(buildTelemetry(intent))
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        awaitClose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    suspend fun readDeviceSpecs(): DeviceSpecs = withContext(Dispatchers.IO) {
        val coreCount = Runtime.getRuntime().availableProcessors()
        val coreFreqs = readCpuCoreFrequencies(coreCount)

        val socMfg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MANUFACTURER.takeIf { it != Build.UNKNOWN && it.isNotBlank() } ?: Build.HARDWARE
        } else {
            Build.HARDWARE
        }
        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it != Build.UNKNOWN && it.isNotBlank() } ?: Build.BOARD
        } else {
            Build.BOARD
        }

        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL).map { sensor ->
            SensorSpec(
                name = sensor.name,
                vendor = sensor.vendor,
                typeName = sensor.stringType.substringAfterLast('.').replace('_', ' '),
                powerMa = sensor.power,
                maxRange = sensor.maximumRange
            )
        }

        DeviceSpecs(
            brand = Build.BRAND.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            device = Build.DEVICE,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            socManufacturer = socMfg,
            socModel = socModel,
            abis = Build.SUPPORTED_ABIS.toList(),
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH,
            buildId = Build.ID,
            kernelVersion = System.getProperty("os.version") ?: "-",
            cpuCores = coreCount,
            coreFrequencies = coreFreqs,
            uptimeMinutes = SystemClock.elapsedRealtime() / 60_000L,
            sensors = sensors
        )
    }

    suspend fun readCpuCoreFrequencies(coreCount: Int = Runtime.getRuntime().availableProcessors()): List<CpuCoreFreq> =
        withContext(Dispatchers.IO) {
            (0 until coreCount).map { idx ->
                val base = "/sys/devices/system/cpu/cpu$idx/cpufreq"
                val curKhz = readSysfsInt("$base/scaling_cur_freq")
                val minKhz = readSysfsInt("$base/cpuinfo_min_freq")
                val maxKhz = readSysfsInt("$base/cpuinfo_max_freq")
                val gov = readSysfsText("$base/scaling_governor")
                CpuCoreFreq(
                    coreIndex = idx,
                    currentMhz = curKhz?.div(1000),
                    minMhz = minKhz?.div(1000),
                    maxMhz = maxKhz?.div(1000),
                    governor = gov
                )
            }
        }

    private fun readSysfsInt(path: String): Int? = runCatching {
        File(path).readText().trim().toIntOrNull()
    }.getOrNull()

    private fun readSysfsText(path: String): String? = runCatching {
        File(path).readText().trim().takeIf { it.isNotBlank() }
    }.getOrNull()
}
