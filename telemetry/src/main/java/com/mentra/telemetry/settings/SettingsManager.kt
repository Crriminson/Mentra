package com.mentra.telemetry.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Settings(
    val overlayNudgesEnabled: Boolean = true,
    val hapticAlertsEnabled: Boolean = true,
    val inputDelayEnabled: Boolean = true,
    val maxUnlocksPerHour: Int = 20,
    val maxScreenTimeMinutes: Int = 120,
    val nudgeIntervalMinutes: Int = 30
)

class SettingsManager(private val context: Context) {
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
        
        private object PreferencesKeys {
            val OVERLAY_NUDGES = booleanPreferencesKey("overlay_nudges")
            val HAPTIC_ALERTS = booleanPreferencesKey("haptic_alerts")
            val INPUT_DELAY = booleanPreferencesKey("input_delay")
            val MAX_UNLOCKS = intPreferencesKey("max_unlocks")
            val MAX_SCREEN_TIME = intPreferencesKey("max_screen_time")
            val NUDGE_INTERVAL = intPreferencesKey("nudge_interval")
        }
    }
    
    val settings: Flow<Settings> = context.dataStore.data.map { preferences ->
        Settings(
            overlayNudgesEnabled = preferences[PreferencesKeys.OVERLAY_NUDGES] ?: true,
            hapticAlertsEnabled = preferences[PreferencesKeys.HAPTIC_ALERTS] ?: true,
            inputDelayEnabled = preferences[PreferencesKeys.INPUT_DELAY] ?: true,
            maxUnlocksPerHour = preferences[PreferencesKeys.MAX_UNLOCKS] ?: 20,
            maxScreenTimeMinutes = preferences[PreferencesKeys.MAX_SCREEN_TIME] ?: 120,
            nudgeIntervalMinutes = preferences[PreferencesKeys.NUDGE_INTERVAL] ?: 30
        )
    }
    
    suspend fun updateOverlayNudges(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERLAY_NUDGES] = enabled
        }
    }
    
    suspend fun updateHapticAlerts(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_ALERTS] = enabled
        }
    }
    
    suspend fun updateInputDelay(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INPUT_DELAY] = enabled
        }
    }
    
    suspend fun updateMaxUnlocks(value: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAX_UNLOCKS] = value.coerceIn(1, 100)
        }
    }
    
    suspend fun updateMaxScreenTime(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.MAX_SCREEN_TIME] = minutes.coerceIn(1, 720)
        }
    }
    
    suspend fun updateNudgeInterval(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NUDGE_INTERVAL] = minutes.coerceIn(1, 120)
        }
    }
} 