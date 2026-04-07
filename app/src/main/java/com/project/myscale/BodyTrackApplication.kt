package com.project.myscale

import android.app.Application
import com.project.myscale.data.local.database.BodyTrackDatabase
import com.project.myscale.data.local.preferences.UserPreferencesManager
import com.project.myscale.data.repository.BodyTrackRepository

class BodyTrackApplication : Application() {

    lateinit var database: BodyTrackDatabase
        private set
    lateinit var repository: BodyTrackRepository
        private set
    lateinit var preferencesManager: UserPreferencesManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = BodyTrackDatabase.getInstance(this)
        repository = BodyTrackRepository(
            entryDao = database.entryDao(),
            measurementValueDao = database.measurementValueDao()
        )
        preferencesManager = UserPreferencesManager(this)
    }
}
