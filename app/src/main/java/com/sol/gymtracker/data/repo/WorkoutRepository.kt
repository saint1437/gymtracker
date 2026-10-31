package com.sol.gymtracker.data.repo

import android.content.Context
import com.sol.gymtracker.data.db.AppSettingsEntity
import com.sol.gymtracker.data.db.ExerciseEntity
import com.sol.gymtracker.data.db.LoggedSetEntity
import com.sol.gymtracker.data.db.PlanEntity
import com.sol.gymtracker.data.db.PlanExerciseEntity
import com.sol.gymtracker.data.db.SessionExerciseEntity
import com.sol.gymtracker.data.db.SyncQueueEntity
import com.sol.gymtracker.data.db.WorkoutDatabase
import com.sol.gymtracker.data.db.WorkoutSessionEntity
import com.sol.gymtracker.data.seed.SeedCatalog
import com.sol.gymtracker.domain.ProgressionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.max

data class PlannedExercise(
    val exercise: ExerciseEntity,
    val sets: Int = exercise.targetSets,
    val repMin: Int = exercise.repMin,
    val repMax: Int = exercise.repMax,
    val rir: Int = exercise.targetRir,
    val restSeconds: Int = exercise.restSeconds,
)

data class ActiveExercise(
    val sessionExercise: SessionExerciseEntity,
    val exercise: ExerciseEntity,
    val sets: List<LoggedSetEntity>,
    val previousSets: List<LoggedSetEntity>,
) {
    val tonnage: Double get() = sets.filter { it.completed }.sumOf { it.tonnage }
    val isProgressionReady: Boolean
        get() = ProgressionEngine.decide(previousSets, sessionExercise.targetSets, sessionExercise.repMax, exercise.defaultStepKg).ready
}

data class ActiveWorkout(
    val session: WorkoutSessionEntity,
    val exercises: List<ActiveExercise>,
) {
    val tonnage: Double get() = exercises.sumOf { it.tonnage }
    val completedSets: Int get() = exercises.sumOf { exercise -> exercise.sets.count { it.completed } }
    val totalSets: Int get() = exercises.sumOf { it.sets.size }
}

class WorkoutRepository(private val context: Context) {
    private val db = WorkoutDatabase.get(context)
    private val exerciseDao = db.exercises()
    private val planDao = db.plans()
    private val sessionDao = db.sessions()
    private val syncDao = db.sync()
    private val settingsDao = db.settings()

    suspend fun seed() = withContext(Dispatchers.IO) { SeedCatalog.seedIfNeeded(db) }

    fun observeExercises(): Flow<List<ExerciseEntity>> = exerciseDao.observeAll()
    fun observeExercises(group: String): Flow<List<ExerciseEntity>> = exerciseDao.observeByGroup(group)
    fun observePlans(): Flow<List<PlanEntity>> = planDao.observeAll()
    fun observeRecentSessions(): Flow<List<WorkoutSessionEntity>> = sessionDao.observeRecent()
    fun observeSettings(): Flow<AppSettingsEntity?> = settingsDao.observe()

    suspend fun getExercisesForPlan(planId: String): List<PlannedExercise> = withContext(Dispatchers.IO) {
        val result = mutableListOf<PlannedExercise>()
        for (relation in planDao.getExercises(planId)) {
            val exercise = exerciseDao.getById(relation.exerciseId) ?: continue
            result += PlannedExercise(exercise, relation.sets, relation.repMin, relation.repMax, relation.targetRir, relation.restSeconds)
        }
        result
    }

    suspend fun getPlan(planId: String): PlanEntity? = planDao.getById(planId)

    suspend fun allExercises(): List<ExerciseEntity> = withContext(Dispatchers.IO) { exerciseDao.getAll() }
    suspend fun allPlans(): List<PlanEntity> = withContext(Dispatchers.IO) { planDao.getAll() }
    suspend fun allPlanExercises(): List<PlanExerciseEntity> = withContext(Dispatchers.IO) { planDao.getAllExercises() }
    suspend fun currentSettings(): AppSettingsEntity = withContext(Dispatchers.IO) { settingsDao.get() ?: AppSettingsEntity() }

    suspend fun createCustomExercise(
        name: String,
        muscleGroup: String,
        equipment: String,
        repMin: Int = 8,
        repMax: Int = 12,
        targetSets: Int = 3,
        targetRir: Int = 2,
        restSeconds: Int = 120,
        note: String = "Работай чисто, без рывков",
    ): String = withContext(Dispatchers.IO) {
        val id = "custom_${UUID.randomUUID()}"
        exerciseDao.insert(ExerciseEntity(id, name, muscleGroup, equipment = equipment, targetSets = targetSets, repMin = repMin, repMax = repMax, targetRir = targetRir, restSeconds = restSeconds, techniqueNote = note, illustrationKey = "custom_$muscleGroup", isCustom = true))
        id
    }

    suspend fun savePlan(name: String, exerciseIds: List<String>): String = withContext(Dispatchers.IO) {
        val planned = exerciseIds.mapNotNull { exerciseDao.getById(it)?.let(::PlannedExercise) }
        savePlanWithExercises(name, planned)
    }

    suspend fun savePlanWithExercises(name: String, planned: List<PlannedExercise>): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        planDao.insert(PlanEntity(id, name, ""))
        val relations = mutableListOf<PlanExerciseEntity>()
        for ((index, item) in planned.withIndex()) {
            relations += PlanExerciseEntity(id, item.exercise.id, index, item.sets, item.repMin, item.repMax, item.rir, item.restSeconds)
        }
        planDao.insertExercises(relations)
        id
    }

    suspend fun updatePlan(planId: String, name: String, planned: List<PlannedExercise>) = withContext(Dispatchers.IO) {
        val current = planDao.getById(planId) ?: return@withContext
        planDao.insert(current.copy(name = name, updatedAt = System.currentTimeMillis()))
        planDao.deleteExercises(planId)
        planDao.insertExercises(planned.mapIndexed { index, item ->
            PlanExerciseEntity(planId, item.exercise.id, index, item.sets, item.repMin, item.repMax, item.rir, item.restSeconds)
        })
    }

    suspend fun reorderPlans(planIds: List<String>) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        planIds.forEachIndexed { index, id -> planDao.updateOrder(id, now - index) }
    }

    suspend fun createSession(
        title: String,
        planId: String?,
        planned: List<PlannedExercise>,
    ): String = withContext(Dispatchers.IO) {
        val sessionId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        sessionDao.insertSession(WorkoutSessionEntity(sessionId, planId, title, cycleType = title.takeIf { it.contains("Full Body") }, startedAt = now))
        val sessionExercises = planned.mapIndexed { index, item ->
            SessionExerciseEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                exerciseId = item.exercise.id,
                orderIndex = index,
                targetSets = item.sets,
                repMin = item.repMin,
                repMax = item.repMax,
                targetRir = item.rir,
                restSeconds = item.restSeconds,
            )
        }
        sessionDao.insertExercises(sessionExercises)
        val initialSets = mutableListOf<LoggedSetEntity>()
        for ((exerciseIndex, sessionExercise) in sessionExercises.withIndex()) {
            val item = planned[exerciseIndex]
            val previous = previousSets(item.exercise.id)
            for (setIndex in 0 until item.sets) {
                val old = previous.getOrNull(setIndex)
                initialSets += LoggedSetEntity(
                    id = UUID.randomUUID().toString(),
                    sessionExerciseId = sessionExercise.id,
                    setIndex = setIndex,
                    weightKg = old?.weightKg ?: 0.0,
                    reps = old?.reps ?: item.repMin,
                    rir = old?.rir ?: item.rir,
                )
            }
        }
        sessionDao.insertSets(initialSets)
        queueSessionChange(sessionId, "CREATE")
        sessionId
    }

    suspend fun addExerciseToSession(sessionId: String, planned: PlannedExercise, orderIndex: Int): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        sessionDao.insertExercises(listOf(SessionExerciseEntity(id, sessionId, planned.exercise.id, orderIndex, planned.sets, planned.repMin, planned.repMax, planned.rir, planned.restSeconds)))
        val previous = previousSets(planned.exercise.id)
        sessionDao.insertSets((0 until planned.sets).map { index ->
            val old = previous.getOrNull(index)
            LoggedSetEntity(UUID.randomUUID().toString(), id, index, old?.weightKg ?: 0.0, old?.reps ?: planned.repMin, old?.rir ?: planned.rir)
        })
        queueSessionChange(sessionId, "UPDATE")
        id
    }

    suspend fun getActiveWorkout(sessionId: String): ActiveWorkout? = withContext(Dispatchers.IO) {
        val session = sessionDao.getById(sessionId) ?: return@withContext null
        val exercises = mutableListOf<ActiveExercise>()
        for (sessionExercise in sessionDao.getExercises(sessionId)) {
            val exercise = exerciseDao.getById(sessionExercise.exerciseId) ?: continue
            val previous = previousSets(exercise.id)
            exercises += ActiveExercise(sessionExercise, exercise, sessionDao.getSets(sessionExercise.id), previous)
        }
        ActiveWorkout(session, exercises)
    }

    suspend fun getWorkoutDetail(sessionId: String): ActiveWorkout? = withContext(Dispatchers.IO) {
        getWorkout(sessionId)
    }

    suspend fun replaceExerciseInSession(sessionExerciseId: String, exercise: ExerciseEntity): ActiveWorkout? = withContext(Dispatchers.IO) {
        val current = sessionDao.getExercise(sessionExerciseId) ?: return@withContext null
        sessionDao.deleteSets(sessionExerciseId)
        sessionDao.updateExercise(
            current.copy(
                exerciseId = exercise.id,
                repMin = exercise.repMin,
                repMax = exercise.repMax,
                targetRir = exercise.targetRir,
                restSeconds = exercise.restSeconds,
                startedAt = null,
                endedAt = null,
                status = "PENDING",
            ),
        )
        sessionDao.insertSets((0 until current.targetSets).map { setIndex ->
            LoggedSetEntity(
                id = UUID.randomUUID().toString(),
                sessionExerciseId = sessionExerciseId,
                setIndex = setIndex,
                weightKg = 0.0,
                reps = exercise.repMin,
                rir = exercise.targetRir,
            )
        })
        queueSessionChange(current.sessionId, "UPDATE")
        getActiveWorkout(current.sessionId)
    }

    suspend fun getActiveSession(): WorkoutSessionEntity? = withContext(Dispatchers.IO) { sessionDao.getActive() }

    suspend fun updateSet(set: LoggedSetEntity, sessionExerciseId: String, complete: Boolean): ActiveWorkout? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val allSets = sessionDao.getSets(sessionExerciseId)
        val previousCompleted = allSets.filter { it.completed && it.id != set.id }.maxByOrNull { it.completedAt ?: 0L }
        val updated = set.copy(
            completed = complete,
            startedAt = set.startedAt ?: now,
            completedAt = if (complete) now else null,
            restBeforeSec = if (complete && previousCompleted?.completedAt != null) max(0, (now - previousCompleted.completedAt) / 1000) else set.restBeforeSec,
            tonnage = if (complete) set.weightKg * set.reps else 0.0,
        )
        sessionDao.updateSet(updated)
        val sessionExercise = sessionDao.getExercise(sessionExerciseId) ?: return@withContext null
        val existing = sessionDao.getSets(sessionExerciseId)
        val started = sessionExercise.copy(startedAt = sessionExercise.startedAt ?: now, status = "IN_PROGRESS")
        val ended = if (existing.all { it.completed }) started.copy(endedAt = now, status = "COMPLETED") else started
        sessionDao.updateExercise(ended)
        val session = sessionDao.getById(sessionExercise.sessionId) ?: return@withContext null
        val allExerciseRows = sessionDao.getExercises(session.id)
        var total = 0.0
        for (row in allExerciseRows) {
            total += sessionDao.getSets(row.id).filter { it.completed }.sumOf { it.tonnage }
        }
        sessionDao.updateSession(session.copy(totalTonnage = total, updatedAt = now))
        queueSessionChange(session.id, "UPDATE")
        getActiveWorkout(session.id) ?: getWorkout(session.id)
    }

    suspend fun completeSession(sessionId: String, notes: String = ""): ActiveWorkout? = withContext(Dispatchers.IO) {
        val session = sessionDao.getById(sessionId) ?: return@withContext null
        val now = System.currentTimeMillis()
        val rows = sessionDao.getExercises(sessionId)
        for (row in rows) {
            val sets = sessionDao.getSets(row.id)
            if (sets.any { it.completed }) sessionDao.updateExercise(row.copy(status = "COMPLETED", endedAt = row.endedAt ?: now))
        }
        var total = 0.0
        for (row in rows) {
            total += sessionDao.getSets(row.id).filter { it.completed }.sumOf { it.tonnage }
        }
        sessionDao.updateSession(session.copy(status = "COMPLETED", endedAt = now, activeDurationSec = (now - session.startedAt) / 1000, totalTonnage = total, notes = notes, updatedAt = now))
        queueSessionChange(sessionId, "UPDATE")
        getWorkout(sessionId)
    }

    suspend fun updateSettings(settings: AppSettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.save(settings)
        context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE).edit()
            .putLong(SETTINGS_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    suspend fun enqueueBackup(payload: String) = withContext(Dispatchers.IO) {
        syncDao.insert(SyncQueueEntity(UUID.randomUUID().toString(), "BACKUP", "database", "UPSERT", payload))
    }

    suspend fun pendingSync() = withContext(Dispatchers.IO) { syncDao.pending() }
    suspend fun markSynced(id: String) = withContext(Dispatchers.IO) { syncDao.markSynced(id) }
    suspend fun markSyncFailed(id: String, error: String) = withContext(Dispatchers.IO) { syncDao.markFailed(id, error) }

    /** Restores completed sessions from a versioned backup. Seeded exercises are kept local; stable IDs merge safely. */
    suspend fun restoreFromJson(payload: String): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(payload)
        restoreSyncedSettings(root)
        val exerciseRows = root.optJSONArray("exercises") ?: JSONArray()
        for (index in 0 until exerciseRows.length()) {
            val row = exerciseRows.optJSONObject(index) ?: continue
            val id = row.optString("id")
            if (id.isBlank()) continue
            if (exerciseDao.getById(id) == null) exerciseDao.insert(
                ExerciseEntity(
                    id = id,
                    name = row.optString("name", "Упражнение"),
                    muscleGroup = row.optString("muscleGroup", "Другое"),
                    secondaryMuscles = row.optString("secondaryMuscles"),
                    equipment = row.optString("equipment", "Другое"),
                    targetSets = row.optInt("targetSets", 3),
                    repMin = row.optInt("repMin", 8),
                    repMax = row.optInt("repMax", 12),
                    targetRir = row.optInt("targetRir", 2),
                    restSeconds = row.optInt("restSeconds", 120),
                    techniqueNote = row.optString("techniqueNote"),
                    illustrationKey = row.optString("illustrationKey", "custom"),
                    loadType = row.optString("loadType", "WEIGHT"),
                    bilateral = row.optBoolean("bilateral", true),
                    defaultStepKg = row.optDouble("defaultStepKg", 0.5),
                    isCustom = row.optBoolean("isCustom", false),
                    isFavorite = row.optBoolean("isFavorite", false),
                    createdAt = row.optLong("createdAt", System.currentTimeMillis()),
                ),
            )
        }
        val skippedPlans = mutableSetOf<String>()
        val replacedPlans = mutableSetOf<String>()
        val planRows = root.optJSONArray("plans") ?: JSONArray()
        for (index in 0 until planRows.length()) {
            val row = planRows.optJSONObject(index) ?: continue
            val id = row.optString("id")
            if (id.isNotBlank()) {
                val incomingUpdatedAt = row.optLong("updatedAt", row.optLong("createdAt", System.currentTimeMillis()))
                val localPlan = planDao.getById(id)
                if (localPlan != null && localPlan.updatedAt >= incomingUpdatedAt) {
                    skippedPlans += id
                } else {
                    planDao.insert(PlanEntity(id, row.optString("name", "План"), row.optString("description"), row.optBoolean("isPreset"), row.optLong("createdAt", System.currentTimeMillis()), incomingUpdatedAt))
                    replacedPlans += id
                }
            }
        }
        val planExerciseRows = root.optJSONArray("planExercises") ?: JSONArray()
        replacedPlans.forEach { planDao.deleteExercises(it) }
        for (index in 0 until planExerciseRows.length()) {
            val row = planExerciseRows.optJSONObject(index) ?: continue
            val planId = row.optString("planId")
            if (planId !in skippedPlans) {
                planDao.insertExercises(listOf(PlanExerciseEntity(planId, row.optString("exerciseId"), row.optInt("orderIndex"), row.optInt("sets", 3), row.optInt("repMin", 8), row.optInt("repMax", 12), row.optInt("targetRir", 2), row.optInt("restSeconds", 120))))
            }
        }
        val sessions = root.optJSONArray("sessions") ?: return@withContext 0
        var restored = 0
        for (index in 0 until sessions.length()) {
            val sessionJson = sessions.optJSONObject(index) ?: continue
            val sessionId = sessionJson.optString("id")
            if (sessionId.isBlank()) continue
            val startedAt = sessionJson.optLong("startedAt", System.currentTimeMillis())
            val endedAt = sessionJson.optLong("endedAt", startedAt).takeIf { it > 0L }
            val incomingUpdatedAt = sessionJson.optLong("updatedAt", endedAt ?: startedAt)
            val localSession = sessionDao.getById(sessionId)
            if (localSession != null && localSession.updatedAt >= incomingUpdatedAt) continue
            if (localSession != null) sessionDao.deleteSession(sessionId)
            sessionDao.insertSession(
                WorkoutSessionEntity(
                    id = sessionId,
                    title = sessionJson.optString("title", "Импортированная тренировка"),
                    cycleType = sessionJson.optString("cycleType").ifBlank { null },
                    startedAt = startedAt,
                    endedAt = endedAt,
                    status = sessionJson.optString("status", "COMPLETED"),
                    totalTonnage = sessionJson.optDouble("tonnage", 0.0),
                    activeDurationSec = sessionJson.optLong("durationSec", 0L),
                    notes = sessionJson.optString("notes"),
                    updatedAt = incomingUpdatedAt,
                ),
            )
            val exerciseRows = sessionJson.optJSONArray("exercises") ?: JSONArray()
            for (exerciseIndex in 0 until exerciseRows.length()) {
                val row = exerciseRows.optJSONObject(exerciseIndex) ?: continue
                val exerciseId = row.optString("id")
                val exercise = exerciseDao.getById(exerciseId) ?: continue
                val sessionExerciseId = row.optString("sessionExerciseId").ifBlank { "$sessionId-$exerciseId-$exerciseIndex" }
                val targetSets = row.optInt("targetSets", exercise.targetSets)
                sessionDao.insertExercises(listOf(
                    SessionExerciseEntity(
                        id = sessionExerciseId,
                        sessionId = sessionId,
                        exerciseId = exercise.id,
                        orderIndex = exerciseIndex,
                        targetSets = targetSets,
                        repMin = row.optInt("repMin", exercise.repMin),
                        repMax = row.optInt("repMax", exercise.repMax),
                        targetRir = row.optInt("targetRir", exercise.targetRir),
                        restSeconds = row.optInt("restSeconds", exercise.restSeconds),
                        status = row.optString("status", "COMPLETED"),
                    ),
                ))
                val sets = row.optJSONArray("sets") ?: JSONArray()
                for (setIndex in 0 until sets.length()) {
                    val setJson = sets.optJSONObject(setIndex) ?: continue
                    val setId = setJson.optString("id").ifBlank { "$sessionExerciseId-set-$setIndex" }
                    sessionDao.insertSets(listOf(
                        LoggedSetEntity(
                            id = setId,
                            sessionExerciseId = sessionExerciseId,
                            setIndex = setIndex,
                            weightKg = setJson.optDouble("weightKg", 0.0),
                            reps = setJson.optInt("reps", 0),
                            rir = setJson.optInt("rir", exercise.targetRir),
                            completed = setJson.optBoolean("completed", true),
                            completedAt = setJson.optLong("completedAt", endedAt ?: startedAt),
                            restBeforeSec = setJson.optLong("restBeforeSec", 0L),
                            tonnage = setJson.optDouble("tonnage", 0.0),
                        ),
                    ))
                }
            }
            restored++
        }
        restored
    }

    private suspend fun restoreSyncedSettings(root: JSONObject) {
        val syncPrefs = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
        val incomingSettings = root.optJSONObject("appSettings")
        if (incomingSettings != null) {
            val incomingUpdatedAt = incomingSettings.optLong("updatedAt", 0L)
            val localUpdatedAt = syncPrefs.getLong(SETTINGS_UPDATED_AT, 0L)
            if (incomingUpdatedAt > localUpdatedAt) {
                val local = settingsDao.get() ?: AppSettingsEntity()
                settingsDao.save(
                    local.copy(
                        defaultStepKg = incomingSettings.optDouble("defaultStepKg", local.defaultStepKg),
                        useMicroStep = incomingSettings.optBoolean("useMicroStep", local.useMicroStep),
                        autoBackup = incomingSettings.optBoolean("autoBackup", local.autoBackup),
                    ),
                )
                syncPrefs.edit().putLong(SETTINGS_UPDATED_AT, incomingUpdatedAt).apply()
            }
        }

        val incomingUi = root.optJSONObject("uiState")
        if (incomingUi != null) {
            val incomingUpdatedAt = incomingUi.optLong("updatedAt", 0L)
            val localUpdatedAt = syncPrefs.getLong(UI_UPDATED_AT, 0L)
            if (incomingUpdatedAt > localUpdatedAt) {
                val pinned = mutableSetOf<String>()
                val rows = incomingUi.optJSONArray("pinnedPlanIds") ?: JSONArray()
                for (index in 0 until rows.length()) rows.optString(index).takeIf { it.isNotBlank() }?.let(pinned::add)
                context.getSharedPreferences(UI_PREFS, Context.MODE_PRIVATE).edit()
                    .putBoolean("pinned_initialized", incomingUi.optBoolean("pinnedConfigured", false))
                    .putStringSet("pinned_plans", pinned)
                    .apply()
                syncPrefs.edit().putLong(UI_UPDATED_AT, incomingUpdatedAt).apply()
            }
        }
    }

    suspend fun exportRows(): List<ExportSession> = withContext(Dispatchers.IO) {
        val result = mutableListOf<ExportSession>()
        for (session in sessionDao.getAllForExport()) {
            val rows = mutableListOf<ExportExercise>()
            for (relation in sessionDao.getExercises(session.id)) {
                val exercise = exerciseDao.getById(relation.exerciseId) ?: continue
                rows += ExportExercise(exercise, relation, sessionDao.getSets(relation.id))
            }
            result += ExportSession(session, rows)
        }
        result
    }

    private suspend fun previousSets(exerciseId: String): List<LoggedSetEntity> {
        val sessionId = sessionDao.getLastCompletedSessionId(exerciseId) ?: return emptyList()
        return sessionDao.getCompletedSetsForExercise(sessionId, exerciseId)
    }

    private suspend fun getWorkout(sessionId: String): ActiveWorkout? {
        val session = sessionDao.getById(sessionId) ?: return null
        val exercises = mutableListOf<ActiveExercise>()
        for (relation in sessionDao.getExercises(sessionId)) {
            val exercise = exerciseDao.getById(relation.exerciseId) ?: continue
            exercises += ActiveExercise(relation, exercise, sessionDao.getSets(relation.id), previousSets(exercise.id))
        }
        return ActiveWorkout(session, exercises)
    }

    private suspend fun queueSessionChange(sessionId: String, operation: String) {
        syncDao.insert(SyncQueueEntity(UUID.randomUUID().toString(), "SESSION", sessionId, operation, "{" + "\"sessionId\":\"$sessionId\"}"))
    }

    companion object {
        const val UI_PREFS = "gymtracker_ui"
        const val SYNC_PREFS = "gymtracker_sync"
        const val SETTINGS_UPDATED_AT = "settings_updated_at"
        const val UI_UPDATED_AT = "ui_updated_at"
    }
}

data class ExportSession(val session: WorkoutSessionEntity, val exercises: List<ExportExercise>)
data class ExportExercise(val exercise: ExerciseEntity, val sessionExercise: SessionExerciseEntity, val sets: List<LoggedSetEntity>)