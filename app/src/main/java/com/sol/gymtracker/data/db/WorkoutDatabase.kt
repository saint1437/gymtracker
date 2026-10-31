package com.sol.gymtracker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ExerciseEntity::class,
        PlanEntity::class,
        PlanExerciseEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        LoggedSetEntity::class,
        SyncQueueEntity::class,
        AppSettingsEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun exercises(): ExerciseDao
    abstract fun plans(): PlanDao
    abstract fun sessions(): SessionDao
    abstract fun sync(): SyncDao
    abstract fun settings(): SettingsDao

    companion object {
        @Volatile private var instance: WorkoutDatabase? = null

        fun get(context: Context): WorkoutDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WorkoutDatabase::class.java,
                "gymtracker.db",
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}