package com.studycompanion.app

import android.app.Application
import com.studycompanion.app.core.di.AppContainer
import com.studycompanion.app.core.di.DefaultAppContainer

class StudyCompanionApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        container.focusEngine.registerOwnPackageName(packageName)
    }
}
