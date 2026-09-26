package com.studycompanion.app.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.studycompanion.app.core.database.AppDatabase
import com.studycompanion.app.core.datastore.UserSessionDataStore
import com.studycompanion.app.data.repository.AuthRepositoryImpl
import com.studycompanion.app.data.repository.ProfileRepositoryImpl
import com.studycompanion.app.data.repository.StudyAppRepositoryImpl
import com.studycompanion.app.data.repository.TargetRepositoryImpl
import com.studycompanion.app.domain.repository.AuthState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryBehaviorTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context
    private lateinit var sessionDataStore: UserSessionDataStore
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var profileRepository: ProfileRepositoryImpl
    private lateinit var targetRepository: TargetRepositoryImpl
    private lateinit var studyAppRepository: StudyAppRepositoryImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = AppDatabase.createInMemory(context)
        sessionDataStore = UserSessionDataStore(context)
        authRepository = AuthRepositoryImpl(db.userDao(), sessionDataStore)
        profileRepository = ProfileRepositoryImpl(db.profileDao(), db.profileSettingsDao(), sessionDataStore)
        targetRepository = TargetRepositoryImpl(db.dailyTargetDao())
        studyAppRepository = StudyAppRepositoryImpl(db.studyAppDao(), context)
    }

    @After
    fun tearDown() = runBlocking {
        sessionDataStore.clearSession()
        db.close()
    }

    @Test
    fun `auth flow - signup, login, and logout manage session state`() = runBlocking {
        // Sign up
        val signUpResult = authRepository.signUp("learner@example.com", "secure123")
        assertTrue(signUpResult.isSuccess)
        val user = signUpResult.getOrThrow()
        assertEquals("learner@example.com", user.email)

        // Session is authenticated
        val authStateAfterSignUp = authRepository.authState.first()
        assertTrue(authStateAfterSignUp is AuthState.Authenticated)

        // Logout
        authRepository.logout()
        val authStateAfterLogout = authRepository.authState.first()
        assertTrue(authStateAfterLogout is AuthState.Unauthenticated)

        // Login again
        val loginResult = authRepository.login("learner@example.com", "secure123")
        assertTrue(loginResult.isSuccess)
        val authStateAfterLogin = authRepository.authState.first()
        assertTrue(authStateAfterLogin is AuthState.Authenticated)
    }

    @Test
    fun `profile security - PIN verification, switching, and locked profile access`() = runBlocking {
        val userResult = authRepository.signUp("profilesecurity@example.com", "pass123")
        val user = userResult.getOrThrow()

        // Create Profile 1 with PIN 1234
        val prof1Result = profileRepository.createProfile(user.id, "Alice", "1234")
        assertTrue(prof1Result.isSuccess)
        val prof1 = prof1Result.getOrThrow()

        // Create Profile 2 with PIN 5678
        val prof2Result = profileRepository.createProfile(user.id, "Bob", "5678")
        assertTrue(prof2Result.isSuccess)
        val prof2 = prof2Result.getOrThrow()

        // Verify PINs
        assertTrue(profileRepository.verifyPin(prof1.id, "1234"))
        assertFalse(profileRepository.verifyPin(prof1.id, "0000"))
        assertTrue(profileRepository.verifyPin(prof2.id, "5678"))
        assertFalse(profileRepository.verifyPin(prof2.id, "1234"))

        // Switch to Profile 2 with correct PIN
        val switchSuccess = profileRepository.switchProfile(prof2.id, "5678")
        assertTrue(switchSuccess.isSuccess)
        val activeProf = profileRepository.getActiveProfileSync()
        assertEquals(prof2.id, activeProf?.id)

        // Switch with incorrect PIN fails
        val switchFail = profileRepository.switchProfile(prof1.id, "wrong")
        assertTrue(switchFail.isFailure)
        // Active profile is still Profile 2
        assertEquals(prof2.id, profileRepository.getActiveProfileSync()?.id)

        // Lock Profile
        profileRepository.lockActiveProfile()
        assertNull("Active profile must be null after locking", profileRepository.getActiveProfileSync())
    }

    @Test
    fun `target repository - original target is preserved when adjusted`() = runBlocking {
        val user = authRepository.signUp("targetuser@example.com", "pass123").getOrThrow()
        val profile = profileRepository.createProfile(user.id, "TargetProfile", "1234").getOrThrow()
        val profileId = profile.id
        val dateKey = "2026-09-22"

        // Set initial target: 2 hours (7200 seconds)
        val setRes = targetRepository.setDailyTarget(profileId, dateKey, 7200L)
        assertTrue(setRes.isSuccess)
        val initial = setRes.getOrThrow()
        assertEquals(7200L, initial.originalTargetSeconds)
        assertNull(initial.adjustedTargetSeconds)
        assertEquals(7200L, initial.effectiveTargetSeconds)

        // Modify today's target to 3 hours (10800 seconds)
        val adjustRes = targetRepository.setDailyTarget(profileId, dateKey, 10800L)
        assertTrue(adjustRes.isSuccess)
        val adjusted = adjustRes.getOrThrow()

        // CRITICAL CHECK: originalTargetSeconds must NOT be erased!
        assertEquals("Original target must be preserved at 7200", 7200L, adjusted.originalTargetSeconds)
        assertEquals("Adjusted target must be 10800", 10800L, adjusted.adjustedTargetSeconds)
        assertEquals("Effective target must be 10800", 10800L, adjusted.effectiveTargetSeconds)

        // Verify retrieval from DB
        val retrieved = targetRepository.getTargetSync(profileId, dateKey)
        assertNotNull(retrieved)
        assertEquals(7200L, retrieved?.originalTargetSeconds)
        assertEquals(10800L, retrieved?.adjustedTargetSeconds)
    }

    @Test
    fun `study app repository - persist, enable, disable, and remove apps`() = runBlocking {
        val user = authRepository.signUp("studyappuser@example.com", "pass123").getOrThrow()
        val profile = profileRepository.createProfile(user.id, "AppProfile", "1234").getOrThrow()
        val profileId = profile.id

        // Add app
        val addRes = studyAppRepository.addStudyApp(profileId, "com.study.math", "Math Practice")
        assertTrue(addRes.isSuccess)

        val apps = studyAppRepository.getStudyAppsSync(profileId)
        assertEquals(1, apps.size)
        assertTrue(apps.first().isEnabled)

        // Disable app
        studyAppRepository.setAppEnabled(profileId, "com.study.math", false)
        val appsAfterDisable = studyAppRepository.getStudyAppsSync(profileId)
        assertFalse(appsAfterDisable.first().isEnabled)

        // Remove app
        studyAppRepository.removeStudyApp(profileId, "com.study.math")
        val appsAfterRemove = studyAppRepository.getStudyAppsSync(profileId)
        assertTrue(appsAfterRemove.isEmpty())
    }
}
