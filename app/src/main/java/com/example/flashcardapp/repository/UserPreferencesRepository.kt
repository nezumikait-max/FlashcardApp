package com.example.flashcardapp.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
open class UserPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object PreferencesKeys {
        val SELECTED_CATEGORY = stringPreferencesKey("selected_category")
    }

    val selectedCategoryFlow: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SELECTED_CATEGORY]
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
}
