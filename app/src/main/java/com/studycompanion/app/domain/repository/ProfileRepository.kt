package com.studycompanion.app.domain.repository

import com.studycompanion.app.domain.model.Profile
import com.studycompanion.app.domain.model.ProfileSettings
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getProfiles(userId: String): Flow<List<Profile>>
    fun getProfile(profileId: String): Flow<Profile?>
    suspend fun createProfile(userId: String, name: String, pin: String): Result<Profile>
    suspend fun verifyPin(profileId: String, pin: String): Boolean
    suspend fun updatePin(profileId: String, oldPin: String, newPin: String): Result<Unit>
    suspend fun deleteProfile(profileId: String): Result<Unit>

    fun getActiveProfile(): Flow<Profile?>
    suspend fun getActiveProfileSync(): Profile?
    suspend fun activateProfile(profileId: String): Result<Profile>
    suspend fun switchProfile(profileId: String, pin: String): Result<Profile>
    suspend fun lockActiveProfile()

    fun getProfileSettings(profileId: String): Flow<ProfileSettings>
    suspend fun updateProfileSettings(settings: ProfileSettings): Result<Unit>
}
