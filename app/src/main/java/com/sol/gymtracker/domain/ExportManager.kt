package com.sol.gymtracker.domain

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.sol.gymtracker.data.repo.ExportSession
import com.sol.gymtracker.data.repo.WorkoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportManager(private val context: Context, private val repository: WorkoutRepository) {
    suspend fun csv(): String = withContext(Dispatchers.IO) {
        val rows = repository.exportRows()
        buildString {
            appendLine("Дата,День цикла,Упражнение,Группа мышц,Целевые подходы,Целевые повторения,RIR (Запас),Подход 1 (Вес x Повт),Подход 2 (Вес x Повт),Подход 3 (Вес x Повт),Заметки по технике и самочувствию")
            rows.forEach { session -> session.exercises.forEach { row ->
                val date = formatDate(session.session.startedAt)
                val sets = row.sets.filter { it.completed }.take(3).map { "${formatNumber(it.weightKg)} кг x ${it.reps}" }
                val cycle = session.session.cycleType ?: "Ручная"
                val targetReps = "${row.sessionExercise.repMin}-${row.sessionExercise.repMax}"
                val values = listOf(
                    date, cycle, row.exercise.name, row.exercise.muscleGroup,
                    row.sessionExercise.targetSets.toString(), targetReps, row.sessionExercise.targetRir.toString(),
                    sets.getOrNull(0).orEmpty(), sets.getOrNull(1).orEmpty(), sets.getOrNull(2).orEmpty(),
                    row.sessionExercise.notes.ifBlank { session.session.notes.ifBlank { row.exercise.techniqueNote } },
                )
                appendLine(values.joinToString(",") { csvCell(it) })
            } }
        }
    }

    suspend fun json(): String = withContext(Dispatchers.IO) {
        val rows = repository.exportRows()
        val exercises = repository.allExercises()
        val plans = repository.allPlans()
        val planExercises = repository.allPlanExercises()
        buildString {
            append("{\"schemaVersion\":1,\"createdAt\":\"${formatDate(System.currentTimeMillis())}\",\"exercises\":[")
            exercises.forEachIndexed { index, exercise ->
                if (index > 0) append(',')
                append("{\"id\":\"${escape(exercise.id)}\",\"name\":\"${escape(exercise.name)}\",\"muscleGroup\":\"${escape(exercise.muscleGroup)}\",\"secondaryMuscles\":\"${escape(exercise.secondaryMuscles)}\",\"equipment\":\"${escape(exercise.equipment)}\",\"targetSets\":${exercise.targetSets},\"repMin\":${exercise.repMin},\"repMax\":${exercise.repMax},\"targetRir\":${exercise.targetRir},\"restSeconds\":${exercise.restSeconds},\"techniqueNote\":\"${escape(exercise.techniqueNote)}\",\"illustrationKey\":\"${escape(exercise.illustrationKey)}\",\"loadType\":\"${escape(exercise.loadType)}\",\"bilateral\":${exercise.bilateral},\"defaultStepKg\":${exercise.defaultStepKg},\"isCustom\":${exercise.isCustom},\"isFavorite\":${exercise.isFavorite},\"createdAt\":${exercise.createdAt}}")
            }
            append("],\"plans\":[")
            plans.forEachIndexed { index, plan ->
                if (index > 0) append(',')
                append("{\"id\":\"${escape(plan.id)}\",\"name\":\"${escape(plan.name)}\",\"description\":\"${escape(plan.description)}\",\"isPreset\":${plan.isPreset},\"createdAt\":${plan.createdAt},\"updatedAt\":${plan.updatedAt}}")
            }
            append("],\"planExercises\":[")
            planExercises.forEachIndexed { index, relation ->
                if (index > 0) append(',')
                append("{\"planId\":\"${escape(relation.planId)}\",\"exerciseId\":\"${escape(relation.exerciseId)}\",\"orderIndex\":${relation.orderIndex},\"sets\":${relation.sets},\"repMin\":${relation.repMin},\"repMax\":${relation.repMax},\"targetRir\":${relation.targetRir},\"restSeconds\":${relation.restSeconds}}")
            }
            append("],\"sessions\":[")
            rows.forEachIndexed { index, session ->
                if (index > 0) append(',')
                appendSession(session)
            }
            append("]}")
        }
    }

    suspend fun write(uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(content) }
                ?: error("Не удалось открыть файл")
            true
        }.getOrDefault(false)
    }

    fun writeInternalBackup(content: String): Boolean = runCatching {
        val directory = context.filesDir.resolve("backups").apply { mkdirs() }
        val file = directory.resolve("workout_backup_latest.json")
        file.writeText(content, Charsets.UTF_8)
        true
    }.getOrDefault(false)

    fun writeShareCache(filename: String, content: String): Uri? = runCatching {
        val directory = context.cacheDir.resolve("exports").apply { mkdirs() }
        val file = directory.resolve(filename).apply { writeText(content, Charsets.UTF_8) }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()

    private fun StringBuilder.appendSession(session: ExportSession) {
            append("{\"id\":\"${escape(session.session.id)}\",\"title\":\"${escape(session.session.title)}\",\"cycleType\":\"${escape(session.session.cycleType.orEmpty())}\",\"startedAt\":${session.session.startedAt},\"endedAt\":${session.session.endedAt ?: 0},\"updatedAt\":${session.session.updatedAt},\"tonnage\":${session.session.totalTonnage},\"durationSec\":${session.session.activeDurationSec},\"status\":\"${session.session.status}\",\"notes\":\"${escape(session.session.notes)}\",\"exercises\":[")
        session.exercises.forEachIndexed { index, row ->
            if (index > 0) append(',')
            append("{\"id\":\"${escape(row.exercise.id)}\",\"sessionExerciseId\":\"${escape(row.sessionExercise.id)}\",\"name\":\"${escape(row.exercise.name)}\",\"group\":\"${escape(row.exercise.muscleGroup)}\",\"targetSets\":${row.sessionExercise.targetSets},\"repMin\":${row.sessionExercise.repMin},\"repMax\":${row.sessionExercise.repMax},\"targetRir\":${row.sessionExercise.targetRir},\"restSeconds\":${row.sessionExercise.restSeconds},\"status\":\"${row.sessionExercise.status}\",\"sets\":[")
            row.sets.forEachIndexed { setIndex, set ->
                if (setIndex > 0) append(',')
                append("{\"id\":\"${escape(set.id)}\",\"weightKg\":${set.weightKg},\"reps\":${set.reps},\"rir\":${set.rir},\"tonnage\":${set.tonnage},\"restBeforeSec\":${set.restBeforeSec ?: 0},\"completedAt\":${set.completedAt ?: 0},\"completed\":${set.completed}}")
            }
            append("]}")
        }
        append("]}")
    }

    private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"").replace("\n", " ")}\""
    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    private fun formatDate(time: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale("ru")).format(Date(time))
    private fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
}