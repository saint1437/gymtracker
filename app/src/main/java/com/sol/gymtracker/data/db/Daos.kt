package com.sol.gymtracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY isFavorite DESC, name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY name")
    suspend fun getAll(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE muscleGroup = :group ORDER BY isFavorite DESC, name")
    fun observeByGroup(group: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' OR muscleGroup LIKE '%' || :query || '%' ORDER BY name")
    fun observeSearch(query: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ExerciseEntity)

    @Update
    suspend fun update(item: ExerciseEntity)

    @Query("SELECT id FROM exercises")
    suspend fun getIds(): List<String>

    @Query("UPDATE exercises SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)
}

@Dao
interface PlanDao {
    @Query("SELECT * FROM plans ORDER BY isPreset DESC, updatedAt DESC")
    fun observeAll(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans ORDER BY updatedAt DESC")
    suspend fun getAll(): List<PlanEntity>

    @Query("SELECT * FROM plan_exercises ORDER BY planId, orderIndex")
    suspend fun getAllExercises(): List<PlanExerciseEntity>

    @Query("SELECT * FROM plans WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PlanEntity?

    @Query("SELECT * FROM plan_exercises WHERE planId = :planId ORDER BY orderIndex")
    suspend fun getExercises(planId: String): List<PlanExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: PlanEntity)

    @Query("UPDATE plans SET updatedAt = :updatedAt WHERE id = :planId")
    suspend fun updateOrder(planId: String, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(items: List<PlanExerciseEntity>)

    @Query("DELETE FROM plan_exercises WHERE planId = :planId")
    suspend fun deleteExercises(planId: String)

    @Query("DELETE FROM plans WHERE id = :planId AND isPreset = 0")
    suspend fun delete(planId: String)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM workout_sessions WHERE status = 'ACTIVE' ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActive(): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE status = 'COMPLETED' ORDER BY endedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int = 20): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    suspend fun getAllForExport(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): WorkoutSessionEntity?

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSession(id: String)

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY orderIndex")
    suspend fun getExercises(sessionId: String): List<SessionExerciseEntity>

    @Query("SELECT * FROM session_exercises WHERE id = :id LIMIT 1")
    suspend fun getExercise(id: String): SessionExerciseEntity?

    @Query("SELECT * FROM logged_sets WHERE sessionExerciseId = :sessionExerciseId ORDER BY setIndex")
    suspend fun getSets(sessionExerciseId: String): List<LoggedSetEntity>

    @Query("DELETE FROM logged_sets WHERE sessionExerciseId = :sessionExerciseId")
    suspend fun deleteSets(sessionExerciseId: String)

    @Query("SELECT ls.* FROM logged_sets ls INNER JOIN session_exercises se ON se.id = ls.sessionExerciseId INNER JOIN workout_sessions ws ON ws.id = se.sessionId WHERE se.exerciseId = :exerciseId AND ws.status = 'COMPLETED' AND ls.completed = 1 ORDER BY ws.endedAt DESC, ls.setIndex")
    suspend fun getLastCompletedSets(exerciseId: String): List<LoggedSetEntity>

    @Query("SELECT ws.id FROM workout_sessions ws INNER JOIN session_exercises se ON se.sessionId = ws.id WHERE se.exerciseId = :exerciseId AND ws.status = 'COMPLETED' ORDER BY ws.endedAt DESC LIMIT 1")
    suspend fun getLastCompletedSessionId(exerciseId: String): String?

    @Query("SELECT ls.* FROM logged_sets ls INNER JOIN session_exercises se ON se.id = ls.sessionExerciseId WHERE se.sessionId = :sessionId AND se.exerciseId = :exerciseId AND ls.completed = 1 ORDER BY ls.setIndex")
    suspend fun getCompletedSetsForExercise(sessionId: String, exerciseId: String): List<LoggedSetEntity>

    @Query("SELECT COUNT(*) FROM workout_sessions WHERE status = 'COMPLETED'")
    suspend fun completedCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(item: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(items: List<SessionExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(items: List<LoggedSetEntity>)

    @Update
    suspend fun updateSession(item: WorkoutSessionEntity)

    @Update
    suspend fun updateExercise(item: SessionExerciseEntity)

    @Update
    suspend fun updateSet(item: LoggedSetEntity)
}

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_queue WHERE synced = 0 ORDER BY createdAt")
    suspend fun pending(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity)

    @Query("UPDATE sync_queue SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE sync_queue SET attempts = attempts + 1, lastError = :error WHERE id = :id")
    suspend fun markFailed(id: String, error: String)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun get(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(item: AppSettingsEntity)
}