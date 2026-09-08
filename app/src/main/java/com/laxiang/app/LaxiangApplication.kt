package com.laxiang.app

import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.laxiang.app.data.LaxiangDatabase
import com.laxiang.app.data.LaxiangRepository

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE records ADD COLUMN stoolColor TEXT")
        db.execSQL("ALTER TABLE records ADD COLUMN stoolAmount TEXT")
        db.execSQL("ALTER TABLE records ADD COLUMN stoolSmell TEXT")
        db.execSQL("ALTER TABLE records ADD COLUMN bowelFeelings TEXT")
    }
}

class LaxiangApplication : Application() {
    lateinit var repository: LaxiangRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = Room.databaseBuilder(
            this,
            LaxiangDatabase::class.java,
            "laxiang.db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
        repository = LaxiangRepository(this, database, database.recordDao(), database.settingsDao())
    }
}
