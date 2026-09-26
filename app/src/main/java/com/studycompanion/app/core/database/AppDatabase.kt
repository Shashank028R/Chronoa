package com.studycompanion.app.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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

@Database(
    entities = [
        UserEntity::class,
        ProfileEntity::class,
        ProfileSettingsEntity::class,
        DeviceEntity::class,
        StudyAppEntity::class,
        DailyTargetEntity::class,
        StudySessionEntity::class,
        SessionEventEntity::class,
        DailyStatsEntity::class,
        SyncMutationEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun profileDao(): ProfileDao
    abstract fun profileSettingsDao(): ProfileSettingsDao
    abstract fun deviceDao(): DeviceDao
    abstract fun studyAppDao(): StudyAppDao
    abstract fun dailyTargetDao(): DailyTargetDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun sessionEventDao(): SessionEventDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun syncMutationDao(): SyncMutationDao

    companion object {
        private const val DATABASE_NAME = "study_companion.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build()
        }

        fun createInMemory(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
            .allowMainThreadQueries()
            .build()
        }
    }
}
