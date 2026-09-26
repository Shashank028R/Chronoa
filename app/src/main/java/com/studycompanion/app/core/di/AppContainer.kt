package com.studycompanion.app.core.di

import android.content.Context
import com.studycompanion.app.core.database.AppDatabase
import com.studycompanion.app.core.datastore.UserSessionDataStore
import com.studycompanion.app.data.repository.AuthRepositoryImpl
import com.studycompanion.app.data.repository.ProfileRepositoryImpl
import com.studycompanion.app.data.repository.SessionRepositoryImpl
import com.studycompanion.app.data.repository.StudyAppRepositoryImpl
import com.studycompanion.app.data.repository.TargetRepositoryImpl
import com.studycompanion.app.domain.repository.AuthRepository
import com.studycompanion.app.domain.repository.ProfileRepository
import com.studycompanion.app.domain.repository.SessionRepository
import com.studycompanion.app.domain.repository.StudyAppRepository
import com.studycompanion.app.domain.repository.TargetRepository

interface AppContainer {
    val database: AppDatabase
    val sessionDataStore: UserSessionDataStore
    val authRepository: AuthRepository
    val profileRepository: ProfileRepository
    val targetRepository: TargetRepository
    val studyAppRepository: StudyAppRepository
    val sessionRepository: SessionRepository
    val apiService: com.studycompanion.app.core.network.ApiService
    val remoteDataSource: com.studycompanion.app.data.remote.RemoteDataSource
    val syncEngine: com.studycompanion.app.sync.SyncEngine
    val syncScheduler: com.studycompanion.app.sync.SyncScheduler
    val statsRepository: com.studycompanion.app.domain.repository.StatsRepository
    val focusEngine: com.studycompanion.app.tracking.engine.FocusEngine
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    override val sessionDataStore: UserSessionDataStore by lazy {
        UserSessionDataStore(context)
    }

    override val apiService: com.studycompanion.app.core.network.ApiService by lazy {
        com.studycompanion.app.core.network.HttpApiService()
    }

    override val remoteDataSource: com.studycompanion.app.data.remote.RemoteDataSource by lazy {
        com.studycompanion.app.data.remote.RemoteDataSource(apiService, sessionDataStore)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(database.userDao(), sessionDataStore, remoteDataSource)
    }

    override val profileRepository: ProfileRepository by lazy {
        ProfileRepositoryImpl(database.profileDao(), database.profileSettingsDao(), sessionDataStore)
    }

    override val targetRepository: TargetRepository by lazy {
        TargetRepositoryImpl(database.dailyTargetDao(), database.syncMutationDao())
    }

    override val studyAppRepository: StudyAppRepository by lazy {
        StudyAppRepositoryImpl(database.studyAppDao(), context, database.syncMutationDao())
    }

    override val sessionRepository: SessionRepository by lazy {
        SessionRepositoryImpl(database.studySessionDao(), database.sessionEventDao(), database.syncMutationDao())
    }

    override val syncEngine: com.studycompanion.app.sync.SyncEngine by lazy {
        com.studycompanion.app.sync.SyncEngine(database, remoteDataSource, sessionDataStore)
    }

    override val syncScheduler: com.studycompanion.app.sync.SyncScheduler by lazy {
        com.studycompanion.app.sync.SyncScheduler(context)
    }

    override val statsRepository: com.studycompanion.app.domain.repository.StatsRepository by lazy {
        com.studycompanion.app.data.repository.StatsRepositoryImpl(
            database.studySessionDao(),
            database.dailyTargetDao(),
            database.studyAppDao()
        )
    }

    override val focusEngine: com.studycompanion.app.tracking.engine.FocusEngine by lazy {
        com.studycompanion.app.tracking.engine.FocusEngine(
            sessionRepository = sessionRepository,
            userSessionDataStore = sessionDataStore
        )
    }
}
