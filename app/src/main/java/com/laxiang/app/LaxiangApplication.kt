package com.laxiang.app

import android.app.Application
import androidx.room.Room
import com.laxiang.app.data.LaxiangDatabase
import com.laxiang.app.data.LaxiangRepository

class LaxiangApplication : Application() {
    lateinit var repository: LaxiangRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(
            this,
            LaxiangDatabase::class.java,
            "laxiang.db"
        ).build()
        repository = LaxiangRepository(this, database, database.recordDao(), database.settingsDao())
    }
}
