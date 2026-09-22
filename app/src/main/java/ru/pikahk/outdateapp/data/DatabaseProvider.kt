package ru.pikahk.outdateapp.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
        instance ?: build(context).also { instance = it }
    }

    private fun build(context: Context): AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "outdate.db"
    )
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
