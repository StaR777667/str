package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AutoCleanInterval
import com.example.model.GameProfile
import com.example.model.OptimizerSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.optimizerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "pulseboost_preferences"
)

class OptimizerPreferences(private val context: Context) {

    private object Keys {
        val AUTO_CLEAN_INTERVAL = stringPreferencesKey("auto_clean_interval")
        val PREFERRED_REFRESH_HZ = floatPreferencesKey("preferred_refresh_hz")
        val THERMAL_ALERT_CELSIUS = floatPreferencesKey("thermal_alert_celsius")
        val GAME_PROFILE = stringPreferencesKey("game_profile")
        val LAST_AUTO_CLEAN_MS = longPreferencesKey("last_auto_clean_ms")
        val TOTAL_RECLAIMED_BYTES = longPreferencesKey("total_reclaimed_bytes")
        val COOLING_MODE_ACTIVE = booleanPreferencesKey("cooling_mode_active")
        val ADDED_GAME_PACKAGES = stringSetPreferencesKey("added_game_packages")
    }

    val settingsFlow: Flow<OptimizerSettings> = context.optimizerDataStore.data.map { prefs ->
        val intervalName = prefs[Keys.AUTO_CLEAN_INTERVAL] ?: AutoCleanInterval.ON_OPEN.name
        val profileName = prefs[Keys.GAME_PROFILE] ?: GameProfile.BALANCED.name
        OptimizerSettings(
            autoCleanInterval = AutoCleanInterval.entries.firstOrNull { it.name == intervalName }
                ?: AutoCleanInterval.ON_OPEN,
            preferredRefreshHz = prefs[Keys.PREFERRED_REFRESH_HZ] ?: 0f,
            thermalAlertCelsius = prefs[Keys.THERMAL_ALERT_CELSIUS] ?: 40f,
            gameProfile = GameProfile.entries.firstOrNull { it.name == profileName }
                ?: GameProfile.BALANCED,
            lastAutoCleanMs = prefs[Keys.LAST_AUTO_CLEAN_MS] ?: 0L,
            totalReclaimedBytes = prefs[Keys.TOTAL_RECLAIMED_BYTES] ?: 0L,
            coolingModeActive = prefs[Keys.COOLING_MODE_ACTIVE] ?: false,
            addedGamePackages = prefs[Keys.ADDED_GAME_PACKAGES] ?: emptySet()
        )
    }

    suspend fun setAutoCleanInterval(interval: AutoCleanInterval) {
        context.optimizerDataStore.edit { it[Keys.AUTO_CLEAN_INTERVAL] = interval.name }
    }

    suspend fun setPreferredRefreshHz(hz: Float) {
        context.optimizerDataStore.edit { it[Keys.PREFERRED_REFRESH_HZ] = hz }
    }

    suspend fun setThermalAlertCelsius(tempC: Float) {
        context.optimizerDataStore.edit { it[Keys.THERMAL_ALERT_CELSIUS] = tempC }
    }

    suspend fun setGameProfile(profile: GameProfile) {
        context.optimizerDataStore.edit { it[Keys.GAME_PROFILE] = profile.name }
    }

    suspend fun setCoolingModeActive(active: Boolean) {
        context.optimizerDataStore.edit { it[Keys.COOLING_MODE_ACTIVE] = active }
    }

    suspend fun recordCleanSweep(bytesFreed: Long, timestampMs: Long) {
        context.optimizerDataStore.edit { prefs ->
            prefs[Keys.LAST_AUTO_CLEAN_MS] = timestampMs
            val current = prefs[Keys.TOTAL_RECLAIMED_BYTES] ?: 0L
            prefs[Keys.TOTAL_RECLAIMED_BYTES] = current + bytesFreed.coerceAtLeast(0L)
        }
    }

    suspend fun toggleGamePackage(packageName: String): Boolean {
        var isAdded = false
        context.optimizerDataStore.edit { prefs ->
            val current = prefs[Keys.ADDED_GAME_PACKAGES] ?: emptySet()
            if (packageName in current) {
                prefs[Keys.ADDED_GAME_PACKAGES] = current - packageName
                isAdded = false
            } else {
                prefs[Keys.ADDED_GAME_PACKAGES] = current + packageName
                isAdded = true
            }
        }
        return isAdded
    }

    suspend fun removeGamePackage(packageName: String) {
        context.optimizerDataStore.edit { prefs ->
            val current = prefs[Keys.ADDED_GAME_PACKAGES] ?: emptySet()
            prefs[Keys.ADDED_GAME_PACKAGES] = current - packageName
        }
    }
}
