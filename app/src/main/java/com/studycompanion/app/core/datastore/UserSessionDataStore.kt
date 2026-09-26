package com.studycompanion.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session_prefs")

class UserSessionDataStore(private val context: Context) {

    private object PreferencesKeys {
        val USER_ID = stringPreferencesKey("user_id")
        val SESSION_TOKEN = stringPreferencesKey("session_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val SYNC_CURSOR = androidx.datastore.preferences.core.longPreferencesKey("sync_cursor")
        val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        val TRACKING_ACTIVE = androidx.datastore.preferences.core.booleanPreferencesKey("tracking_active")
        val ACTIVE_SESSION_MARKER = stringPreferencesKey("active_session_marker")
    }

    val userIdFlow: Flow<String?> = context.userDataStore.data.map { prefs ->
        prefs[PreferencesKeys.USER_ID]
    }

    val sessionTokenFlow: Flow<String?> = context.userDataStore.data.map { prefs ->
        prefs[PreferencesKeys.SESSION_TOKEN]
    }

    val refreshTokenFlow: Flow<String?> = context.userDataStore.data.map { prefs ->
        prefs[PreferencesKeys.REFRESH_TOKEN]
    }

    val syncCursorFlow: Flow<Long> = context.userDataStore.data.map { prefs ->
        prefs[PreferencesKeys.SYNC_CURSOR] ?: 0L
    }

    val activeProfileIdFlow: Flow<String?> = context.userDataStore.data.map { prefs ->
        prefs[PreferencesKeys.ACTIVE_PROFILE_ID]
    }

    val trackingActiveFlow: Flow<Boolean> = context.userDataStore.data.map { prefs ->
        prefs[PreferencesKeys.TRACKING_ACTIVE] ?: false
    }

    val activeSessionMarkerFlow: Flow<String?> = context.userDataStore.data.map { prefs ->
        prefs[PreferencesKeys.ACTIVE_SESSION_MARKER]
    }

    suspend fun saveSession(userId: String, token: String, refreshToken: String? = null) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.USER_ID] = userId
            prefs[PreferencesKeys.SESSION_TOKEN] = token
            if (refreshToken != null) {
                prefs[PreferencesKeys.REFRESH_TOKEN] = refreshToken
            }
        }
    }

    suspend fun saveSyncCursor(cursor: Long) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.SYNC_CURSOR] = cursor
        }
    }

    suspend fun setActiveProfileId(profileId: String?) {
        context.userDataStore.edit { prefs ->
            if (profileId != null) {
                prefs[PreferencesKeys.ACTIVE_PROFILE_ID] = profileId
            } else {
                prefs.remove(PreferencesKeys.ACTIVE_PROFILE_ID)
            }
        }
    }

    suspend fun clearSession() {
        context.userDataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.USER_ID)
            prefs.remove(PreferencesKeys.SESSION_TOKEN)
            prefs.remove(PreferencesKeys.REFRESH_TOKEN)
            prefs.remove(PreferencesKeys.ACTIVE_PROFILE_ID)
        }
    }

    suspend fun clearActiveProfile() {
        context.userDataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.ACTIVE_PROFILE_ID)
        }
    }

    suspend fun setTrackingActive(active: Boolean) {
        context.userDataStore.edit { prefs ->
            prefs[PreferencesKeys.TRACKING_ACTIVE] = active
        }
    }

    suspend fun setActiveSessionMarker(markerJson: String?) {
        context.userDataStore.edit { prefs ->
            if (markerJson != null) {
                prefs[PreferencesKeys.ACTIVE_SESSION_MARKER] = markerJson
            } else {
                prefs.remove(PreferencesKeys.ACTIVE_SESSION_MARKER)
            }
        }
    }

    suspend fun getActiveSessionMarker(): String? {
        return context.userDataStore.data.map { it[PreferencesKeys.ACTIVE_SESSION_MARKER] }.firstOrNull()
    }

    suspend fun isTrackingActive(): Boolean {
        return context.userDataStore.data.map { it[PreferencesKeys.TRACKING_ACTIVE] ?: false }.firstOrNull() ?: false
    }
}
