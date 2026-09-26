# Study Companion — ProGuard & R8 Optimization Rules for Release Candidate

# Room Database & Entities
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <methods>;
}

# Domain & Sync Data Models (prevent field stripping during reflection/JSON)
-keep class com.studycompanion.app.domain.model.** { *; }
-keep class com.studycompanion.app.sync.model.** { *; }
-keep class com.studycompanion.app.core.diagnostics.** { *; }
-keep class com.studycompanion.app.core.platform.** { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# OkHttp & Logging
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# WorkManager
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.InputMerger { *; }

# DataStore
-keep class androidx.datastore.** { *; }
