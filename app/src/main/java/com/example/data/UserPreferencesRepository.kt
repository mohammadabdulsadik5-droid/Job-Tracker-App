package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "job_tracker_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val SEEDED_UIDS = stringSetPreferencesKey("seeded_uids")
    }

    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_THEME] ?: false
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = enabled
        }
    }

    suspend fun isDemoSeeded(uid: String): Boolean {
        val prefs = context.dataStore.data.first()
        return (prefs[Keys.SEEDED_UIDS] ?: emptySet()).contains(uid)
    }

    suspend fun markDemoSeeded(uid: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.SEEDED_UIDS] ?: emptySet()
            prefs[Keys.SEEDED_UIDS] = current + uid
        }
    }
}
