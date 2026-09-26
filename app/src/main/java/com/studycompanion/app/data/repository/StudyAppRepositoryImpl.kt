package com.studycompanion.app.data.repository

import android.content.Context
import android.content.Intent
import com.studycompanion.app.core.database.dao.StudyAppDao
import com.studycompanion.app.core.database.entity.StudyAppEntity
import com.studycompanion.app.domain.model.StudyApp
import com.studycompanion.app.domain.repository.SelectableApp
import com.studycompanion.app.domain.repository.StudyAppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class StudyAppRepositoryImpl(
    private val studyAppDao: StudyAppDao,
    private val context: Context,
    private val syncMutationDao: com.studycompanion.app.core.database.dao.SyncMutationDao? = null,
    private val deviceId: String = "local-device"
) : StudyAppRepository {

    override fun getStudyApps(profileId: String): Flow<List<StudyApp>> {
        return flow {
            emit(studyAppDao.getAppsForProfile(profileId).map { it.toDomain() })
            emitAll(studyAppDao.getAppsFlowForProfile(profileId).map { list ->
                list.map { it.toDomain() }
            })
        }
    }

    override fun getEnabledStudyApps(profileId: String): Flow<List<StudyApp>> {
        return studyAppDao.getEnabledAppsFlowForProfile(profileId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getStudyAppsSync(profileId: String): List<StudyApp> {
        return studyAppDao.getAppsForProfile(profileId).map { it.toDomain() }
    }

    override suspend fun addStudyApp(
        profileId: String,
        packageName: String,
        appLabel: String
    ): Result<StudyApp> {
        val trimmedPkg = packageName.trim()
        val trimmedLabel = appLabel.trim()
        if (trimmedPkg.isBlank()) {
            return Result.failure(IllegalArgumentException("Package name cannot be blank"))
        }

        val now = System.currentTimeMillis()
        val existing = studyAppDao.getApp(profileId, trimmedPkg)

        val entity = if (existing != null) {
            existing.copy(
                appLabel = trimmedLabel.ifBlank { existing.appLabel },
                isEnabled = true,
                updatedAt = now
            )
        } else {
            StudyAppEntity(
                id = UUID.randomUUID().toString(),
                profileId = profileId,
                packageName = trimmedPkg,
                appLabel = trimmedLabel.ifBlank { trimmedPkg },
                iconRef = null,
                isEnabled = true,
                addedAt = now,
                updatedAt = now
            )
        }

        studyAppDao.insert(entity)
        enqueueAppMutation(entity, "UPDATE")
        return Result.success(entity.toDomain())
    }

    override suspend fun removeStudyApp(profileId: String, packageName: String): Result<Unit> {
        val trimmedPkg = packageName.trim()
        studyAppDao.deleteApp(profileId, trimmedPkg)
        val dao = syncMutationDao
        if (dao != null) {
            val payload = org.json.JSONObject().apply {
                put("profileId", profileId)
                put("packageName", trimmedPkg)
            }
            dao.insert(
                com.studycompanion.app.core.database.entity.SyncMutationEntity(
                    id = UUID.randomUUID().toString(),
                    profileId = profileId,
                    deviceId = deviceId,
                    entityType = "STUDY_APP",
                    recordId = "$profileId:$trimmedPkg",
                    operation = "DELETE",
                    payloadJson = payload.toString(),
                    createdAt = System.currentTimeMillis(),
                    syncState = "PENDING"
                )
            )
        }
        return Result.success(Unit)
    }

    override suspend fun setAppEnabled(
        profileId: String,
        packageName: String,
        enabled: Boolean
    ): Result<Unit> {
        val now = System.currentTimeMillis()
        val trimmedPkg = packageName.trim()
        studyAppDao.setEnabled(profileId, trimmedPkg, enabled, now)
        val app = studyAppDao.getApp(profileId, trimmedPkg)
        if (app != null) {
            enqueueAppMutation(app, "UPDATE")
        }
        return Result.success(Unit)
    }

    override suspend fun updateAppLabel(
        profileId: String,
        packageName: String,
        newLabel: String
    ): Result<Unit> {
        val trimmedPkg = packageName.trim()
        val trimmedLabel = newLabel.trim()
        val existing = studyAppDao.getApp(profileId, trimmedPkg) ?: return Result.failure(Exception("App not found"))
        val now = System.currentTimeMillis()
        val updated = existing.copy(appLabel = trimmedLabel, updatedAt = now)
        studyAppDao.insert(updated)
        enqueueAppMutation(updated, "UPDATE")
        return Result.success(Unit)
    }

    private suspend fun enqueueAppMutation(entity: StudyAppEntity, operation: String) {
        val dao = syncMutationDao ?: return
        val payload = org.json.JSONObject().apply {
            put("id", entity.id)
            put("profileId", entity.profileId)
            put("packageName", entity.packageName)
            put("appLabel", entity.appLabel)
            put("isEnabled", entity.isEnabled)
            put("addedAt", entity.addedAt)
            put("updatedAt", entity.updatedAt)
        }
        val mutation = com.studycompanion.app.core.database.entity.SyncMutationEntity(
            id = UUID.randomUUID().toString(),
            profileId = entity.profileId,
            deviceId = deviceId,
            entityType = "STUDY_APP",
            recordId = entity.id,
            operation = operation,
            payloadJson = payload.toString(),
            createdAt = entity.updatedAt,
            syncState = "PENDING"
        )
        dao.insert(mutation)
    }

    override suspend fun getAvailableLaunchableApps(profileId: String): List<SelectableApp> {
        val pm = context.packageManager
        val selectedPackages = studyAppDao.getAppsForProfile(profileId).map { it.packageName }.toSet()

        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = try {
            pm.queryIntentActivities(launcherIntent, 0)
        } catch (_: Exception) {
            emptyList()
        }

        val appsMap = mutableMapOf<String, SelectableApp>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo?.packageName ?: continue
            if (pkg == context.packageName) continue // Exclude own app
            val label = try {
                resolveInfo.loadLabel(pm).toString()
            } catch (_: Exception) {
                pkg
            }
            appsMap[pkg] = SelectableApp(
                packageName = pkg,
                label = label.ifBlank { pkg },
                isSelected = selectedPackages.contains(pkg)
            )
        }

        // Also query installed applications with a launch intent to ensure any non-standard launcher activities or installed user apps are included
        try {
            val installedApps = pm.getInstalledApplications(0)
            for (appInfo in installedApps) {
                val pkg = appInfo.packageName ?: continue
                if (pkg == context.packageName) continue
                if (!appsMap.containsKey(pkg)) {
                    val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    if (launchIntent != null) {
                        val label = try {
                            appInfo.loadLabel(pm).toString()
                        } catch (_: Exception) {
                            pkg
                        }
                        appsMap[pkg] = SelectableApp(
                            packageName = pkg,
                            label = label.ifBlank { pkg },
                            isSelected = selectedPackages.contains(pkg)
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore
        }

        return appsMap.values.sortedBy { it.label.lowercase() }
    }

    private fun StudyAppEntity.toDomain(): StudyApp {
        return StudyApp(
            id = id,
            profileId = profileId,
            packageName = packageName,
            appLabel = appLabel,
            iconRef = iconRef,
            isEnabled = isEnabled,
            addedAt = addedAt,
            updatedAt = updatedAt
        )
    }
}
