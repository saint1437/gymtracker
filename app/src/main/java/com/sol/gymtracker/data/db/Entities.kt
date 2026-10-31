package com.sol.gymtracker.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,
    val secondaryMuscles: String = "",
    val equipment: String,
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRir: Int,
    val restSeconds: Int,
    val techniqueNote: String,
    val illustrationKey: String,
    val loadType: String = "WEIGHT",
    val bilateral: Boolean = true,
    val defaultStepKg: Double = 0.5,
    val isCustom: Boolean = false,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "plans")
data class PlanEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val description: String = "",
    val isPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "plan_exercises",
    primaryKeys = ["planId", "exerciseId"],
    foreignKeys = [
        ForeignKey(entity = PlanEntity::class, parentColumns = ["id"], childColumns = ["planId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("planId"), Index("exerciseId")],
)
data class PlanExerciseEntity(
    val planId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRir: Int,
    val restSeconds: Int,
)

@Entity(tableName = "workout_sessions", indices = [Index("status"), Index("startedAt")])
data class WorkoutSessionEntity(
    @androidx.room.PrimaryKey val id: String,
    val planId: String? = null,
    val title: String,
    val cycleType: String? = null,
    val startedAt: Long,
    val endedAt: Long? = null,
    val status: String = "ACTIVE",
    val totalTonnage: Double = 0.0,
    val activeDurationSec: Long = 0,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(entity = WorkoutSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SessionExerciseEntity(
    @androidx.room.PrimaryKey val id: String,
    val sessionId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRir: Int,
    val restSeconds: Int,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val status: String = "PENDING",
    val notes: String = "",
)

@Entity(
    tableName = "logged_sets",
    foreignKeys = [ForeignKey(entity = SessionExerciseEntity::class, parentColumns = ["id"], childColumns = ["sessionExerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionExerciseId"), Index("completed")],
)
data class LoggedSetEntity(
    @androidx.room.PrimaryKey val id: String,
    val sessionExerciseId: String,
    val setIndex: Int,
    val weightKg: Double,
    val reps: Int,
    val rir: Int,
    val completed: Boolean = false,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val restBeforeSec: Long? = null,
    val tonnage: Double = 0.0,
    val note: String = "",
)

@Entity(tableName = "sync_queue", indices = [Index("synced"), Index("createdAt")])
data class SyncQueueEntity(
    @androidx.room.PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val payload: String,
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val lastError: String? = null,
    val synced: Boolean = false,
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @androidx.room.PrimaryKey val id: Int = 1,
    val defaultStepKg: Double = 0.5,
    val useMicroStep: Boolean = false,
    val driveAccount: String? = null,
    val driveFolderId: String? = null,
    val syncEnabled: Boolean = false,
    val autoBackup: Boolean = true,
    val lastSyncAt: Long? = null,
    val lastBackupAt: Long? = null,
)