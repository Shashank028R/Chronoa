package com.studycompanion.app.data.repository

import com.studycompanion.app.core.database.dao.ProfileDao
import com.studycompanion.app.core.database.dao.ProfileSettingsDao
import com.studycompanion.app.core.database.entity.ProfileEntity
import com.studycompanion.app.core.database.entity.ProfileSettingsEntity
import com.studycompanion.app.core.datastore.UserSessionDataStore
import com.studycompanion.app.core.security.PinVerifier
import com.studycompanion.app.domain.model.AnimationLevel
import com.studycompanion.app.domain.model.FocusDisplayMode
import com.studycompanion.app.domain.model.Profile
import com.studycompanion.app.domain.model.ProfileSettings
import com.studycompanion.app.domain.model.ThemeMode
import com.studycompanion.app.domain.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileRepositoryImpl(
    private val profileDao: ProfileDao,
    private val profileSettingsDao: ProfileSettingsDao,
    private val sessionDataStore: UserSessionDataStore
) : ProfileRepository {

    private val inMemoryActiveProfile = MutableStateFlow<Profile?>(null)

    override fun getProfiles(userId: String): Flow<List<Profile>> {
        return profileDao.getProfilesFlowForUser(userId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getProfile(profileId: String): Flow<Profile?> {
        return profileDao.getProfileFlow(profileId).map { it?.toDomain() }
    }

    override suspend fun createProfile(userId: String, name: String, pin: String): Result<Profile> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return Result.failure(IllegalArgumentException("Profile name cannot be blank"))
        }
        val trimmedPin = pin.trim()
        if (!PinVerifier.isValidPinFormat(trimmedPin)) {
            return Result.failure(IllegalArgumentException("PIN must be 4 to 6 digits"))
        }

        val profileId = UUID.randomUUID().toString()
        val (salt, verifier) = PinVerifier.hashPin(trimmedPin)
        val now = System.currentTimeMillis()

        val profileEntity = ProfileEntity(
            id = profileId,
            userId = userId,
            name = trimmedName,
            avatarRef = null,
            pinSalt = salt,
            pinVerifier = verifier,
            pinVersion = 1,
            createdAt = now,
            updatedAt = now,
            deletedAt = null
        )

        val defaultSettings = ProfileSettingsEntity(
            profileId = profileId,
            countWhileLocked = true,
            pauseDuringCalls = true,
            blockNotifications = false,
            pauseInMultiWindow = true,
            pauseInFloatingWindow = true,
            defaultDailyTargetSeconds = 10800L,
            focusDisplayMode = "MINIMAL",
            animationLevel = "STANDARD",
            themeMode = "SYSTEM",
            updatedAt = now
        )

        profileDao.insert(profileEntity)
        profileSettingsDao.insert(defaultSettings)

        val domain = profileEntity.toDomain()
        val currentActive = inMemoryActiveProfile.value ?: sessionDataStore.activeProfileIdFlow.firstOrNull()
        if (currentActive == null) {
            inMemoryActiveProfile.value = domain
            sessionDataStore.setActiveProfileId(profileId)
        }

        return Result.success(domain)
    }

    override suspend fun verifyPin(profileId: String, pin: String): Boolean {
        val entity = profileDao.getProfileById(profileId) ?: return false
        return PinVerifier.verifyPin(pin.trim(), entity.pinSalt, entity.pinVerifier)
    }

    override suspend fun updatePin(profileId: String, oldPin: String, newPin: String): Result<Unit> {
        val entity = profileDao.getProfileById(profileId)
            ?: return Result.failure(IllegalArgumentException("Profile not found"))

        if (!PinVerifier.verifyPin(oldPin.trim(), entity.pinSalt, entity.pinVerifier)) {
            return Result.failure(SecurityException("Incorrect current PIN"))
        }

        val trimmedNewPin = newPin.trim()
        if (!PinVerifier.isValidPinFormat(trimmedNewPin)) {
            return Result.failure(IllegalArgumentException("New PIN must be 4 to 6 digits"))
        }

        val (salt, verifier) = PinVerifier.hashPin(trimmedNewPin)
        val updated = entity.copy(
            pinSalt = salt,
            pinVerifier = verifier,
            updatedAt = System.currentTimeMillis()
        )
        profileDao.update(updated)
        return Result.success(Unit)
    }

    override suspend fun deleteProfile(profileId: String): Result<Unit> {
        val now = System.currentTimeMillis()
        profileDao.softDeleteProfile(profileId, now)
        if (inMemoryActiveProfile.value?.id == profileId) {
            inMemoryActiveProfile.value = null
        }
        val active = sessionDataStore.activeProfileIdFlow.firstOrNull()
        if (active == profileId) {
            sessionDataStore.clearActiveProfile()
        }
        return Result.success(Unit)
    }

    override fun getActiveProfile(): Flow<Profile?> {
        return flow {
            if (inMemoryActiveProfile.value == null) {
                val storedId = sessionDataStore.activeProfileIdFlow.firstOrNull()
                if (storedId != null) {
                    val restored = profileDao.getProfileById(storedId)?.toDomain()
                    if (restored != null) {
                        inMemoryActiveProfile.value = restored
                    }
                }
            }
            emitAll(inMemoryActiveProfile)
        }
    }

    override suspend fun getActiveProfileSync(): Profile? {
        val current = inMemoryActiveProfile.value
        if (current != null) return current
        val activeId = sessionDataStore.activeProfileIdFlow.firstOrNull() ?: return null
        val restored = profileDao.getProfileById(activeId)?.toDomain()
        if (restored != null) {
            inMemoryActiveProfile.value = restored
        }
        return restored
    }

    override suspend fun activateProfile(profileId: String): Result<Profile> {
        val entity = profileDao.getProfileById(profileId)
            ?: return Result.failure(IllegalArgumentException("Profile not found"))
        val domain = entity.toDomain()
        inMemoryActiveProfile.value = domain
        sessionDataStore.setActiveProfileId(profileId)
        return Result.success(domain)
    }

    suspend fun setActiveProfileDirectly(profileId: String?) {
        if (profileId != null) {
            val entity = profileDao.getProfileById(profileId)?.toDomain()
            inMemoryActiveProfile.value = entity
            sessionDataStore.setActiveProfileId(profileId)
        } else {
            inMemoryActiveProfile.value = null
            sessionDataStore.clearActiveProfile()
        }
    }

    override suspend fun switchProfile(profileId: String, pin: String): Result<Profile> {
        val profileEntity = profileDao.getProfileById(profileId)
            ?: return Result.failure(IllegalArgumentException("Profile not found"))

        if (!PinVerifier.verifyPin(pin.trim(), profileEntity.pinSalt, profileEntity.pinVerifier)) {
            return Result.failure(SecurityException("Incorrect PIN for profile ${profileEntity.name}"))
        }

        val domain = profileEntity.toDomain()
        inMemoryActiveProfile.value = domain
        sessionDataStore.setActiveProfileId(profileId)
        return Result.success(domain)
    }

    override suspend fun lockActiveProfile() {
        inMemoryActiveProfile.value = null
        sessionDataStore.clearActiveProfile()
    }

    override fun getProfileSettings(profileId: String): Flow<ProfileSettings> {
        return profileSettingsDao.getSettingsFlow(profileId).map { entity ->
            entity?.toDomain() ?: ProfileSettings(
                profileId = profileId,
                countWhileLocked = true,
                pauseDuringCalls = true,
                blockNotifications = false,
                pauseInMultiWindow = true,
                pauseInFloatingWindow = true,
                defaultDailyTargetSeconds = 10800L,
                focusDisplayMode = FocusDisplayMode.MINIMAL,
                animationLevel = AnimationLevel.STANDARD,
                themeMode = ThemeMode.SYSTEM
            )
        }
    }

    override suspend fun updateProfileSettings(settings: ProfileSettings): Result<Unit> {
        val entity = ProfileSettingsEntity(
            profileId = settings.profileId,
            countWhileLocked = settings.countWhileLocked,
            pauseDuringCalls = settings.pauseDuringCalls,
            blockNotifications = settings.blockNotifications,
            pauseInMultiWindow = settings.pauseInMultiWindow,
            pauseInFloatingWindow = settings.pauseInFloatingWindow,
            defaultDailyTargetSeconds = settings.defaultDailyTargetSeconds,
            focusDisplayMode = settings.focusDisplayMode.name,
            animationLevel = settings.animationLevel.name,
            themeMode = settings.themeMode.name,
            updatedAt = System.currentTimeMillis()
        )
        profileSettingsDao.insert(entity)
        return Result.success(Unit)
    }

    private fun ProfileEntity.toDomain(): Profile {
        return Profile(
            id = id,
            userId = userId,
            name = name,
            avatarRef = avatarRef,
            createdAt = createdAt,
            updatedAt = updatedAt,
            deletedAt = deletedAt
        )
    }

    private fun ProfileSettingsEntity.toDomain(): ProfileSettings {
        val mode = try { FocusDisplayMode.valueOf(focusDisplayMode) } catch (e: Exception) { FocusDisplayMode.MINIMAL }
        val anim = try { AnimationLevel.valueOf(animationLevel) } catch (e: Exception) { AnimationLevel.STANDARD }
        val theme = try { ThemeMode.valueOf(themeMode) } catch (e: Exception) { ThemeMode.SYSTEM }
        return ProfileSettings(
            profileId = profileId,
            countWhileLocked = countWhileLocked,
            pauseDuringCalls = pauseDuringCalls,
            blockNotifications = blockNotifications,
            pauseInMultiWindow = pauseInMultiWindow,
            pauseInFloatingWindow = pauseInFloatingWindow,
            defaultDailyTargetSeconds = defaultDailyTargetSeconds,
            focusDisplayMode = mode,
            animationLevel = anim,
            themeMode = theme,
            updatedAt = updatedAt
        )
    }
}
