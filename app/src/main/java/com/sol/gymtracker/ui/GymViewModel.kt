package com.sol.gymtracker.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sol.gymtracker.data.db.AppSettingsEntity
import com.sol.gymtracker.data.db.ExerciseEntity
import com.sol.gymtracker.data.db.LoggedSetEntity
import com.sol.gymtracker.data.db.PlanEntity
import com.sol.gymtracker.data.repo.ActiveWorkout
import com.sol.gymtracker.data.repo.PlannedExercise
import com.sol.gymtracker.data.repo.WorkoutRepository
import com.sol.gymtracker.sync.GitHubRelease
import com.sol.gymtracker.sync.GitHubReleaseChecker
import com.sol.gymtracker.domain.ExportManager
import com.sol.gymtracker.sync.DriveSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GymViewModel(application: Application) : AndroidViewModel(application) {
    val repository = WorkoutRepository(application)

    val exercises: StateFlow<List<ExerciseEntity>> = repository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val plans: StateFlow<List<PlanEntity>> = repository.observePlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val recentSessions = repository.observeRecentSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings: StateFlow<AppSettingsEntity> = repository.observeSettings().map { it ?: AppSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettingsEntity())

    private val _builderIds = MutableStateFlow<List<String>>(emptyList())
    val builderIds: StateFlow<List<String>> = _builderIds.asStateFlow()
    private val _preview = MutableStateFlow<List<PlannedExercise>>(emptyList())
    val preview: StateFlow<List<PlannedExercise>> = _preview.asStateFlow()
    private val _previewTitle = MutableStateFlow("")
    val previewTitle: StateFlow<String> = _previewTitle.asStateFlow()
    private val _previewPlanId = MutableStateFlow<String?>(null)
    private val _activeWorkout = MutableStateFlow<ActiveWorkout?>(null)
    val activeWorkout: StateFlow<ActiveWorkout?> = _activeWorkout.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    init {
        viewModelScope.launch { repository.seed() }
        viewModelScope.launch { repository.getActiveSession()?.id?.let { loadActive(it) } }
        DriveSyncWorker.enqueuePeriodic(application)
        checkForUpdates()
    }

    fun clearMessage() { _message.value = null }

    fun toggleBuilderExercise(id: String) {
        _builderIds.value = if (id in _builderIds.value) _builderIds.value - id else _builderIds.value + id
    }

    fun clearBuilder() { _builderIds.value = emptyList() }

    fun removePreviewExercise(exerciseId: String) {
        _preview.value = _preview.value.filterNot { it.exercise.id == exerciseId }
    }

    fun movePreviewExercise(from: Int, to: Int) {
        val current = _preview.value.toMutableList()
        if (from !in current.indices || to !in current.indices) return
        val item = current.removeAt(from)
        current.add(to, item)
        _preview.value = current
    }

    fun prepareManualPreview(name: String) {
        val selected = exercises.value.filter { it.id in _builderIds.value }
        _previewPlanId.value = null
        _previewTitle.value = name.ifBlank { "Своя тренировка" }
        _preview.value = selected.map { PlannedExercise(it) }
    }

    suspend fun preparePlan(plan: PlanEntity) {
        _previewPlanId.value = plan.id
        _previewTitle.value = plan.name
        _preview.value = repository.getExercisesForPlan(plan.id)
    }

    suspend fun startPreparedSession(): String? {
        if (_preview.value.isEmpty()) return null
        return repository.createSession(_previewTitle.value, _previewPlanId.value, _preview.value).also { loadActive(it) }
    }

    suspend fun startPlan(plan: PlanEntity): String? {
        val planned = repository.getExercisesForPlan(plan.id)
        if (planned.isEmpty()) return null
        _previewTitle.value = plan.name
        _preview.value = planned
        return repository.createSession(plan.name, plan.id, planned).also { loadActive(it) }
    }

    suspend fun startManualSession(name: String): String? {
        val selected = exercises.value.filter { it.id in _builderIds.value }.map { PlannedExercise(it) }
        if (selected.isEmpty()) return null
        return repository.createSession(name.ifBlank { "Своя тренировка" }, null, selected).also { loadActive(it) }
    }

    suspend fun saveBuilderPlan(name: String): String? {
        val selected = _builderIds.value
        if (selected.isEmpty()) return null
        return repository.savePlan(name.ifBlank { "Свой план" }, selected)
    }

    suspend fun loadActive(sessionId: String) {
        _activeWorkout.value = repository.getActiveWorkout(sessionId)
    }

    suspend fun updateSet(set: LoggedSetEntity, complete: Boolean) {
        _activeWorkout.value = repository.updateSet(set, set.sessionExerciseId, complete)
        val exporter = ExportManager(getApplication(), repository)
        exporter.writeInternalBackup(exporter.json())
    }

    suspend fun finishSession(notes: String = "") {
        _activeWorkout.value?.session?.id?.let {
            repository.completeSession(it, notes)
            val exporter = ExportManager(getApplication(), repository)
            exporter.writeInternalBackup(exporter.json())
            if (settings.value.syncEnabled) DriveSyncWorker.enqueue(getApplication())
            _activeWorkout.value = null
        }
    }

    suspend fun addExerciseToActive(exercise: ExerciseEntity) {
        val workout = _activeWorkout.value ?: return
        val order = workout.exercises.size
        repository.addExerciseToSession(workout.session.id, PlannedExercise(exercise), order)
        loadActive(workout.session.id)
    }

    fun updateSettings(settings: AppSettingsEntity) {
        viewModelScope.launch { repository.updateSettings(settings) }
    }

    fun showMessage(text: String) { _message.value = text }

    fun checkForUpdates() {
        if (_updateState.value.isChecking || !GitHubReleaseChecker.isConfigured) return
        viewModelScope.launch {
            _updateState.update { it.copy(isChecking = true) }
            val release = GitHubReleaseChecker.latest()
            _updateState.value = UpdateState(
                release = release?.takeIf { it.isNewerThan(com.sol.gymtracker.BuildConfig.VERSION_NAME) },
                isChecking = false,
                checkedAt = System.currentTimeMillis(),
            )
        }
    }
}

data class UpdateState(
    val release: GitHubRelease? = null,
    val isChecking: Boolean = false,
    val checkedAt: Long? = null,
)