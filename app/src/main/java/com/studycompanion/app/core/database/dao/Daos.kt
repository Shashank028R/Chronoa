package com.studycompanion.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studycompanion.app.core.database.entity.DailyStatsEntity
import com.studycompanion.app.core.database.entity.DailyTargetEntity
import com.studycompanion.app.core.database.entity.DeviceEntity
import com.studycompanion.app.core.database.entity.ProfileEntity
import com.studycompanion.app.core.database.entity.ProfileSettingsEntity
import com.studycompanion.app.core.database.entity.SessionEventEntity
import com.studycompanion.app.core.database.entity.StudyAppEntity
import com.studycompanion.app.core.database.entity.StudySessionEntity
import com.studycompanion.app.core.database.entity.SyncMutationEntity
import com.studycompanion.app.core.database.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) AND deleted_at IS NULL LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id AND deleted_at IS NULL")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :id AND deleted_at IS NULL")
    fun getUserFlow(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE deleted_at IS NULL LIMIT 1")
    suspend fun getFirstActiveUser(): UserEntity?

    @Query("SELECT * FROM users WHERE deleted_at IS NULL LIMIT 1")
    fun getFirstActiveUserFlow(): Flow<UserEntity?>

    @Query("UPDATE users SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: Long)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: ProfileEntity)

    @Update
    suspend fun update(profile: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE id = :id AND deleted_at IS NULL")
    suspend fun getProfileById(id: String): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id AND deleted_at IS NULL")
    fun getProfileFlow(id: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE user_id = :userId AND deleted_at IS NULL ORDER BY created_at ASC")
    suspend fun getProfilesForUser(userId: String): List<ProfileEntity>

    @Query("SELECT * FROM profiles WHERE user_id = :userId AND deleted_at IS NULL ORDER BY created_at ASC")
    fun getProfilesFlowForUser(userId: String): Flow<List<ProfileEntity>>

    @Query("UPDATE profiles SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDeleteProfile(id: String, deletedAt: Long)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun hardDeleteProfile(id: String)
}

@Dao
interface ProfileSettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: ProfileSettingsEntity)

    @Update
    suspend fun update(settings: ProfileSettingsEntity)

    @Query("SELECT * FROM profile_settings WHERE profile_id = :profileId")
    suspend fun getSettings(profileId: String): ProfileSettingsEntity?

    @Query("SELECT * FROM profile_settings WHERE profile_id = :profileId")
    fun getSettingsFlow(profileId: String): Flow<ProfileSettingsEntity?>

    @Query("DELETE FROM profile_settings WHERE profile_id = :profileId")
    suspend fun deleteSettings(profileId: String)
}

@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceEntity)

    @Query("SELECT * FROM devices WHERE user_id = :userId")
    suspend fun getDevicesForUser(userId: String): List<DeviceEntity>

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getDeviceById(id: String): DeviceEntity?

    @Query("UPDATE devices SET last_seen_at = :lastSeenAt WHERE id = :id")
    suspend fun updateLastSeen(id: String, lastSeenAt: Long)
}

@Dao
interface StudyAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: StudyAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<StudyAppEntity>)

    @Update
    suspend fun update(app: StudyAppEntity)

    @Query("SELECT * FROM study_apps WHERE profile_id = :profileId ORDER BY app_label ASC")
    suspend fun getAppsForProfile(profileId: String): List<StudyAppEntity>

    @Query("SELECT * FROM study_apps WHERE profile_id = :profileId ORDER BY app_label ASC")
    fun getAppsFlowForProfile(profileId: String): Flow<List<StudyAppEntity>>

    @Query("SELECT * FROM study_apps WHERE profile_id = :profileId AND is_enabled = 1")
    suspend fun getEnabledAppsForProfile(profileId: String): List<StudyAppEntity>

    @Query("SELECT * FROM study_apps WHERE profile_id = :profileId AND is_enabled = 1")
    fun getEnabledAppsFlowForProfile(profileId: String): Flow<List<StudyAppEntity>>

    @Query("SELECT * FROM study_apps WHERE profile_id = :profileId AND package_name = :packageName")
    suspend fun getApp(profileId: String, packageName: String): StudyAppEntity?

    @Query("DELETE FROM study_apps WHERE profile_id = :profileId AND package_name = :packageName")
    suspend fun deleteApp(profileId: String, packageName: String)

    @Query("DELETE FROM study_apps WHERE profile_id = :profileId")
    suspend fun deleteAppsForProfile(profileId: String)

    @Query("UPDATE study_apps SET is_enabled = :enabled, updated_at = :updatedAt WHERE profile_id = :profileId AND package_name = :packageName")
    suspend fun setEnabled(profileId: String, packageName: String, enabled: Boolean, updatedAt: Long)
}

@Dao
interface DailyTargetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(target: DailyTargetEntity)

    @Update
    suspend fun update(target: DailyTargetEntity)

    @Query("SELECT * FROM daily_targets WHERE profile_id = :profileId AND date_key = :dateKey")
    suspend fun getTarget(profileId: String, dateKey: String): DailyTargetEntity?

    @Query("SELECT * FROM daily_targets WHERE profile_id = :profileId AND date_key = :dateKey")
    fun getTargetFlow(profileId: String, dateKey: String): Flow<DailyTargetEntity?>

    @Query("SELECT * FROM daily_targets WHERE profile_id = :profileId ORDER BY date_key DESC")
    suspend fun getTargetsForProfile(profileId: String): List<DailyTargetEntity>

    @Query("SELECT * FROM daily_targets WHERE profile_id = :profileId ORDER BY date_key DESC")
    fun getTargetsFlowForProfile(profileId: String): Flow<List<DailyTargetEntity>>

    @Query("DELETE FROM daily_targets WHERE id = :id")
    suspend fun deleteTarget(id: String)

    @Query("DELETE FROM daily_targets WHERE profile_id = :profileId")
    suspend fun deleteTargetsForProfile(profileId: String)
}

@Dao
interface StudySessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StudySessionEntity)

    @Update
    suspend fun update(session: StudySessionEntity)

    @Query("SELECT * FROM study_sessions WHERE id = :id AND deleted_at IS NULL")
    suspend fun getSessionById(id: String): StudySessionEntity?

    @Query("SELECT * FROM study_sessions WHERE profile_id = :profileId AND deleted_at IS NULL ORDER BY start_at DESC")
    suspend fun getSessionsForProfile(profileId: String): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE profile_id = :profileId AND deleted_at IS NULL ORDER BY start_at DESC")
    fun getSessionsFlowForProfile(profileId: String): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE profile_id = :profileId AND start_at >= :startAt AND end_at <= :endAt AND deleted_at IS NULL ORDER BY start_at ASC")
    suspend fun getSessionsForProfileInRange(profileId: String, startAt: Long, endAt: Long): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE profile_id = :profileId AND start_at >= :startAt AND end_at <= :endAt AND deleted_at IS NULL ORDER BY start_at ASC")
    fun getSessionsFlowForProfileInRange(profileId: String, startAt: Long, endAt: Long): Flow<List<StudySessionEntity>>

    @Query("UPDATE study_sessions SET deleted_at = :deletedAt WHERE id = :id")
    suspend fun softDeleteSession(id: String, deletedAt: Long)

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun hardDeleteSession(id: String)

    @Query("DELETE FROM study_sessions WHERE profile_id = :profileId")
    suspend fun deleteSessionsForProfile(profileId: String)
}

@Dao
interface SessionEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: SessionEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<SessionEventEntity>)

    @Query("SELECT * FROM session_events WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getEventsForSession(sessionId: String): List<SessionEventEntity>

    @Query("SELECT * FROM session_events WHERE profile_id = :profileId ORDER BY timestamp ASC")
    suspend fun getEventsForProfile(profileId: String): List<SessionEventEntity>
}

@Dao
interface DailyStatsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stats: DailyStatsEntity)

    @Update
    suspend fun update(stats: DailyStatsEntity)

    @Query("SELECT * FROM daily_stats WHERE profile_id = :profileId AND date_key = :dateKey")
    suspend fun getStats(profileId: String, dateKey: String): DailyStatsEntity?

    @Query("SELECT * FROM daily_stats WHERE profile_id = :profileId AND date_key = :dateKey")
    fun getStatsFlow(profileId: String, dateKey: String): Flow<DailyStatsEntity?>

    @Query("SELECT * FROM daily_stats WHERE profile_id = :profileId AND date_key >= :startDate AND date_key <= :endDate ORDER BY date_key ASC")
    suspend fun getStatsRange(profileId: String, startDate: String, endDate: String): List<DailyStatsEntity>

    @Query("DELETE FROM daily_stats WHERE profile_id = :profileId")
    suspend fun deleteStatsForProfile(profileId: String)
}

@Dao
interface SyncMutationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mutation: SyncMutationEntity)

    @Query("SELECT * FROM sync_mutations WHERE profile_id = :profileId AND sync_state = 'PENDING' ORDER BY created_at ASC")
    suspend fun getPendingMutationsForProfile(profileId: String): List<SyncMutationEntity>

    @Query("SELECT * FROM sync_mutations WHERE sync_state = 'PENDING' ORDER BY created_at ASC")
    suspend fun getAllPendingMutations(): List<SyncMutationEntity>

    @Update
    suspend fun update(mutation: SyncMutationEntity)

    @Query("DELETE FROM sync_mutations WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM sync_mutations WHERE id IN (:ids)")
    suspend fun deleteBatch(ids: List<String>)

    @Query("SELECT COUNT(*) FROM sync_mutations WHERE sync_state = 'PENDING'")
    suspend fun getPendingCount(): Int
}
