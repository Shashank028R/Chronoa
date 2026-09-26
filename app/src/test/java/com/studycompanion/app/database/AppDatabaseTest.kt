package com.studycompanion.app.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.studycompanion.app.core.database.AppDatabase
import com.studycompanion.app.core.database.dao.DailyStatsDao
import com.studycompanion.app.core.database.dao.DailyTargetDao
import com.studycompanion.app.core.database.dao.DeviceDao
import com.studycompanion.app.core.database.dao.ProfileDao
import com.studycompanion.app.core.database.dao.ProfileSettingsDao
import com.studycompanion.app.core.database.dao.SessionEventDao
import com.studycompanion.app.core.database.dao.StudyAppDao
import com.studycompanion.app.core.database.dao.StudySessionDao
import com.studycompanion.app.core.database.dao.SyncMutationDao
import com.studycompanion.app.core.database.dao.UserDao
import com.studycompanion.app.core.database.entity.DailyTargetEntity
import com.studycompanion.app.core.database.entity.ProfileEntity
import com.studycompanion.app.core.database.entity.StudyAppEntity
import com.studycompanion.app.core.database.entity.StudySessionEntity
import com.studycompanion.app.core.database.entity.UserEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var profileDao: ProfileDao
    private lateinit var profileSettingsDao: ProfileSettingsDao
    private lateinit var dailyTargetDao: DailyTargetDao
    private lateinit var studyAppDao: StudyAppDao
    private lateinit var studySessionDao: StudySessionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.createInMemory(context)
        userDao = db.userDao()
        profileDao = db.profileDao()
        profileSettingsDao = db.profileSettingsDao()
        dailyTargetDao = db.dailyTargetDao()
        studyAppDao = db.studyAppDao()
        studySessionDao = db.studySessionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `user insertion and retrieval works`() = runBlocking {
        val user = UserEntity(
            id = UUID.randomUUID().toString(),
            email = "user@example.com",
            authProvider = "EMAIL",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        userDao.insert(user)

        val retrieved = userDao.getUserById(user.id)
        assertNotNull(retrieved)
        assertEquals("user@example.com", retrieved?.email)
    }

    @Test
    fun `profile isolation - Profile A cannot see Profile B targets`() = runBlocking {
        val userId = UUID.randomUUID().toString()
        val user = UserEntity(id = userId, email = "test@example.com", authProvider = "EMAIL", createdAt = 1000L, updatedAt = 1000L)
        userDao.insert(user)

        val profA = ProfileEntity(
            id = "profile-a",
            userId = userId,
            name = "Profile A",
            pinSalt = "saltA",
            pinVerifier = "verifierA",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val profB = ProfileEntity(
            id = "profile-b",
            userId = userId,
            name = "Profile B",
            pinSalt = "saltB",
            pinVerifier = "verifierB",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        profileDao.insert(profA)
        profileDao.insert(profB)

        // Insert target for Profile A
        val targetA = DailyTargetEntity(
            id = "target-a",
            profileId = profA.id,
            dateKey = "2026-09-22",
            originalTargetSeconds = 7200L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        // Insert target for Profile B
        val targetB = DailyTargetEntity(
            id = "target-b",
            profileId = profB.id,
            dateKey = "2026-09-22",
            originalTargetSeconds = 14400L,
            createdAt = 1000L,
            updatedAt = 1000L
        )
        dailyTargetDao.insert(targetA)
        dailyTargetDao.insert(targetB)

        // Querying for Profile A
        val resultA = dailyTargetDao.getTarget(profA.id, "2026-09-22")
        assertNotNull(resultA)
        assertEquals(7200L, resultA?.originalTargetSeconds)
        assertEquals(profA.id, resultA?.profileId)

        // Querying for Profile B
        val resultB = dailyTargetDao.getTarget(profB.id, "2026-09-22")
        assertNotNull(resultB)
        assertEquals(14400L, resultB?.originalTargetSeconds)
        assertEquals(profB.id, resultB?.profileId)

        // Profile A list only contains Profile A's target
        val listA = dailyTargetDao.getTargetsForProfile(profA.id)
        assertEquals(1, listA.size)
        assertEquals("target-a", listA.first().id)
    }

    @Test
    fun `profile isolation - Study Apps and Sessions are isolated per profile`() = runBlocking {
        val userId = UUID.randomUUID().toString()
        userDao.insert(UserEntity(id = userId, email = "isolation@example.com", authProvider = "EMAIL", createdAt = 1000L, updatedAt = 1000L))

        val profA = ProfileEntity(id = "p-a", userId = userId, name = "A", pinSalt = "sA", pinVerifier = "vA", createdAt = 1000L, updatedAt = 1000L)
        val profB = ProfileEntity(id = "p-b", userId = userId, name = "B", pinSalt = "sB", pinVerifier = "vB", createdAt = 1000L, updatedAt = 1000L)
        profileDao.insert(profA)
        profileDao.insert(profB)

        // Study Apps
        studyAppDao.insert(StudyAppEntity(id = "app-a", profileId = profA.id, packageName = "com.app.a", appLabel = "App A", addedAt = 1000L, updatedAt = 1000L))
        studyAppDao.insert(StudyAppEntity(id = "app-b", profileId = profB.id, packageName = "com.app.b", appLabel = "App B", addedAt = 1000L, updatedAt = 1000L))

        val appsA = studyAppDao.getAppsForProfile(profA.id)
        assertEquals(1, appsA.size)
        assertEquals("com.app.a", appsA.first().packageName)

        val appsB = studyAppDao.getAppsForProfile(profB.id)
        assertEquals(1, appsB.size)
        assertEquals("com.app.b", appsB.first().packageName)

        // Study Sessions
        val sessionA = StudySessionEntity(
            id = "sess-a",
            profileId = profA.id,
            deviceId = "dev-1",
            platform = "ANDROID",
            packageName = "com.app.a",
            startAt = 1000L,
            endAt = 2000L,
            durationSeconds = 1000L,
            trackingType = "AUTOMATIC",
            verificationStatus = "VERIFIED_BY_RULES",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        studySessionDao.insert(sessionA)

        // Profile A has session, Profile B has none
        val sessionsA = studySessionDao.getSessionsForProfile(profA.id)
        assertEquals(1, sessionsA.size)

        val sessionsB = studySessionDao.getSessionsForProfile(profB.id)
        assertTrue("Profile B must have zero sessions", sessionsB.isEmpty())
    }

    @Test
    fun `soft delete excludes profile from active queries`() = runBlocking {
        val userId = UUID.randomUUID().toString()
        userDao.insert(UserEntity(id = userId, email = "soft@example.com", authProvider = "EMAIL", createdAt = 1000L, updatedAt = 1000L))

        val prof = ProfileEntity(id = "p-1", userId = userId, name = "Active", pinSalt = "s", pinVerifier = "v", createdAt = 1000L, updatedAt = 1000L)
        profileDao.insert(prof)

        assertNotNull(profileDao.getProfileById("p-1"))

        // Soft delete
        profileDao.softDeleteProfile("p-1", System.currentTimeMillis())

        // Should now be excluded from getProfileById
        assertNull(profileDao.getProfileById("p-1"))
        assertTrue(profileDao.getProfilesForUser(userId).isEmpty())
    }
}
