package com.example.flashcardapp.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    object PreferencesKeys {
        val SELECTED_CATEGORY = stringPreferencesKey("selected_category")
        val AUTO_CLOSE_SECONDS = intPreferencesKey("auto_close_seconds")
        val APPEARANCE_INTERVAL_MINUTES = intPreferencesKey("appearance_interval_minutes")
        val SIDEBAR_SIDE = stringPreferencesKey("sidebar_side")
    }

    val selectedCategoryFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_CATEGORY]
        }

    val autoCloseSecondsFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> 
            preferences[PreferencesKeys.AUTO_CLOSE_SECONDS] ?: 30 
        }

    val appearanceIntervalMinutesFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> 
            preferences[PreferencesKeys.APPEARANCE_INTERVAL_MINUTES] ?: 1 
        }

    val sidebarSideFlow: Flow<String> = context.dataStore.data
        .map { preferences -> 
            preferences[PreferencesKeys.SIDEBAR_SIDE] ?: "Right" 
        }

    suspend fun saveSelectedCategory(category: String?) {
        context.dataStore.edit { preferences ->
            if (category == null) {
                preferences.remove(PreferencesKeys.SELECTED_CATEGORY)
            } else {
                preferences[PreferencesKeys.SELECTED_CATEGORY] = category
            }
        }
    }

    suspend fun saveAutoCloseSeconds(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CLOSE_SECONDS] = seconds
        }
    }

    suspend fun saveAppearanceIntervalMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APPEARANCE_INTERVAL_MINUTES] = minutes
        }
    }

    suspend fun saveSidebarSide(side: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SIDEBAR_SIDE] = side
        }
    }
}
