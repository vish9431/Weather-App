package com.example.weather.Activities


import android.content.Context
import android.widget.Toast
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [LocationEntity::class], version = 5)
abstract class Locationdatabase : RoomDatabase() {
    abstract fun LocationDao(): locationDao

    companion object {
        @Volatile
        private var INSTANCE: Locationdatabase? = null

        fun getDatabase(context: Context): Locationdatabase {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = buildDatabase(context)
                }
            }
            return INSTANCE!!
        }

        private fun buildDatabase(context: Context): Locationdatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                Locationdatabase::class.java,
                "locations_database"
            )
                .build()
        }

    }


}



