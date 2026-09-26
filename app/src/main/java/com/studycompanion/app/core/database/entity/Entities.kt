package com.studycompanion.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey
    val id: String,
    val email: String,
    @ColumnInfo(name = "auth_provider")
    val authProvider: String,
    @ColumnInfo(name = "password_salt")
    val passwordSalt: String = "",
    @ColumnInfo(name = "password_hash")
    val passwordHash: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null
)

@Entity(
    tableName = "profiles",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("user_id")
    ]
)
data class ProfileEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val name: String,
    @ColumnInfo(name = "avatar_ref")
    val avatarRef: String? = null,
    @ColumnInfo(name = "pin_salt")
    val pinSalt: String,
    @ColumnInfo(name = "pin_verifier")
    val pinVerifier: String,
    @ColumnInfo(name = "pin_version")
    val pinVersion: Int = 1,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null
)

@Entity(
    tableName = "profile_settings",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProfileSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "count_while_locked")
    val countWhileLocked: Boolean = true,
    @ColumnInfo(name = "pause_during_calls")
    val pauseDuringCalls: Boolean = true,
    @ColumnInfo(name = "block_notifications")
    val blockNotifications: Boolean = false,
    @ColumnInfo(name = "pause_in_multi_window")
    val pauseInMultiWindow: Boolean = true,
    @ColumnInfo(name = "pause_in_floating_window")
    val pauseInFloatingWindow: Boolean = true,
    @ColumnInfo(name = "default_daily_target_seconds")
    val defaultDailyTargetSeconds: Long = 10800L,
    @ColumnInfo(name = "focus_display_mode")
    val focusDisplayMode: String = "MINIMAL",
    @ColumnInfo(name = "animation_level")
    val animationLevel: String = "STANDARD",
    @ColumnInfo(name = "theme_mode")
    val themeMode: String = "SYSTEM",
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "devices",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("user_id")
    ]
)
data class DeviceEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "user_id")
    val userId: String,
    val platform: String,
    @ColumnInfo(name = "model_label")
    val modelLabel: String,
    @ColumnInfo(name = "os_version")
    val osVersion: String,
    @ColumnInfo(name = "app_version")
    val appVersion: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "last_seen_at")
    val lastSeenAt: Long,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)

@Entity(
    tableName = "study_apps",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profile_id", "package_name"], unique = true),
        Index("profile_id")
    ]
)
data class StudyAppEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "app_label")
    val appLabel: String,
    @ColumnInfo(name = "icon_ref")
    val iconRef: String? = null,
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,
    @ColumnInfo(name = "added_at")
    val addedAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "daily_targets",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profile_id", "date_key"], unique = true),
        Index("profile_id")
    ]
)
data class DailyTargetEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "date_key")
    val dateKey: String,
    @ColumnInfo(name = "original_target_seconds")
    val originalTargetSeconds: Long,
    @ColumnInfo(name = "adjusted_target_seconds")
    val adjustedTargetSeconds: Long? = null,
    @ColumnInfo(name = "carry_in_seconds")
    val carryInSeconds: Long = 0L,
    @ColumnInfo(name = "carry_out_seconds")
    val carryOutSeconds: Long = 0L,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("profile_id"),
        Index(value = ["profile_id", "start_at"]),
        Index("package_name"),
        Index("sync_state")
    ]
)
data class StudySessionEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "device_id")
    val deviceId: String,
    val platform: String,
    @ColumnInfo(name = "package_name")
    val packageName: String?,
    @ColumnInfo(name = "start_at")
    val startAt: Long,
    @ColumnInfo(name = "end_at")
    val endAt: Long,
    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Long,
    @ColumnInfo(name = "tracking_type")
    val trackingType: String,
    @ColumnInfo(name = "verification_status")
    val verificationStatus: String,
    @ColumnInfo(name = "subject_id")
    val subjectId: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    @ColumnInfo(name = "sync_version")
    val syncVersion: Long = 1L,
    @ColumnInfo(name = "sync_state")
    val syncState: String = "LOCAL"
)

@Entity(
    tableName = "session_events",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profile_id", "timestamp"]),
        Index(value = ["session_id", "timestamp"]),
        Index(value = ["event_type", "timestamp"])
    ]
)
data class SessionEventEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String? = null,
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "device_id")
    val deviceId: String,
    @ColumnInfo(name = "event_type")
    val eventType: String,
    val timestamp: Long,
    @ColumnInfo(name = "package_name")
    val packageName: String? = null,
    @ColumnInfo(name = "engine_state")
    val engineState: String,
    @ColumnInfo(name = "reason_code")
    val reasonCode: String? = null,
    val source: String,
    @ColumnInfo(name = "metadata_json")
    val metadataJson: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)

@Entity(
    tableName = "daily_stats",
    foreignKeys = [
        ForeignKey(
            entity = ProfileEntity::class,
            parentColumns = ["id"],
            childColumns = ["profile_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["profile_id", "date_key"], unique = true),
        Index("profile_id")
    ]
)
data class DailyStatsEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "date_key")
    val dateKey: String,
    @ColumnInfo(name = "automatic_seconds")
    val automaticSeconds: Long,
    @ColumnInfo(name = "manual_seconds")
    val manualSeconds: Long,
    @ColumnInfo(name = "total_seconds")
    val totalSeconds: Long,
    @ColumnInfo(name = "effective_target_seconds")
    val effectiveTargetSeconds: Long,
    @ColumnInfo(name = "session_count")
    val sessionCount: Int,
    @ColumnInfo(name = "longest_session_seconds")
    val longestSessionSeconds: Long,
    @ColumnInfo(name = "streak_contribution")
    val streakContribution: Boolean,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "sync_mutations",
    indices = [
        Index("profile_id"),
        Index("sync_state")
    ]
)
data class SyncMutationEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "profile_id")
    val profileId: String,
    @ColumnInfo(name = "device_id")
    val deviceId: String,
    @ColumnInfo(name = "entity_type")
    val entityType: String,
    @ColumnInfo(name = "record_id")
    val recordId: String,
    val operation: String,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "sync_state")
    val syncState: String = "PENDING",
    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int = 0,
    @ColumnInfo(name = "last_attempt_at")
    val lastAttemptAt: Long? = null,
    @ColumnInfo(name = "error_code")
    val errorCode: String? = null
)
