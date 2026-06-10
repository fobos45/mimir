package com.mimir.app

import android.app.Application
import androidx.room.Room
import com.mimir.app.data.AppDatabase

class MimirApplication : Application() {

    lateinit var db: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, AppDatabase::class.java, "mimir.db")
            .fallbackToDestructiveMigration()
            .build()
    }
}
