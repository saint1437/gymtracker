@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.sol.gymtracker.ui

import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.media.ToneGenerator
import android.media.AudioManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sol.gymtracker.AppScreen
import com.sol.gymtracker.BuildConfig
import com.sol.gymtracker.data.db.AppSettingsEntity
import com.sol.gymtracker.data.db.ExerciseEntity
import com.sol.gymtracker.data.db.LoggedSetEntity
import com.sol.gymtracker.data.repo.ActiveExercise
import com.sol.gymtracker.data.repo.ActiveWorkout
import com.sol.gymtracker.data.repo.PlannedExercise
import com.sol.gymtracker.domain.ExportManager
import com.sol.gymtracker.sync.DriveSyncManager
import com.sol.gymtracker.sync.DriveSyncWorker
import com.sol.gymtracker.sync.GitHubRelease
import com.sol.gymtracker.sync.GitHubReleaseChecker
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.sol.gymtracker.R
import com.sol.gymtracker.ui.text.formatDateRu
import com.sol.gymtracker.ui.text.formatDurationRu
import com.sol.gymtracker.ui.text.formatKg
import com.sol.gymtracker.ui.text.formatKgWithUnit
import com.sol.gymtracker.ui.text.formatSetSummary
import com.sol.gymtracker.ui.text.formatTimer
import com.sol.gymtracker.ui.text.previousSetsSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val Groups = listOf("Все", "Ноги", "Икры", "Пресс", "Грудь", "Спина", "Плечи", "Бицепс", "Трицепс")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymTrackerApp(
    viewModel: GymViewModel,
    screen: AppScreen,
    activeSessionId: String?,
    onScreen: (AppScreen) -> Unit,
    onActive: (String) -> Unit,
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val recent by viewModel.recentSessions.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val active by viewModel.activeWorkout.collectAsStateWithLifecycle()
    val builderIds by viewModel.builderIds.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val previewTitle by viewModel.previewTitle.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val message by viewModel.message.collectAsStateWithLifecycle()
    val updateState by viewModel.updateState.collectAsStateWithLifecycle()
    var showUpdateDialog by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); viewModel.clearMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (screen != AppScreen.ACTIVE && screen != AppScreen.PREVIEW) {
                NavigationBar {
                    NavigationBarItem(screen == AppScreen.HOME, { onScreen(AppScreen.HOME) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text(stringResource(R.string.nav_home)) })
                    NavigationBarItem(screen == AppScreen.PLANS || screen == AppScreen.BUILDER, { onScreen(AppScreen.PLANS) }, icon = { Icon(Icons.Default.FitnessCenter, null) }, label = { Text(stringResource(R.string.nav_plans)) })
                    NavigationBarItem(screen == AppScreen.HISTORY, { onScreen(AppScreen.HISTORY) }, icon = { Icon(Icons.Default.History, null) }, label = { Text(stringResource(R.string.nav_history)) })
                    NavigationBarItem(screen == AppScreen.SETTINGS, { onScreen(AppScreen.SETTINGS) }, icon = { NotificationIcon(Icons.Default.Settings, updateState.release != null) }, label = { Text(stringResource(R.string.nav_settings)) })
                }
            }
        },
    ) { padding ->
        when (screen) {
            AppScreen.HOME -> HomeScreen(
                Modifier.padding(padding), plans, recent, active,
                onPlans = { onScreen(AppScreen.PLANS) },
                onSettings = { onScreen(AppScreen.SETTINGS) },
                onStartPlan = { plan -> scope.launch { viewModel.preparePlan(plan); onScreen(AppScreen.PREVIEW) } },
                onResume = { active?.session?.id?.let { scope.launch { viewModel.loadActive(it); onActive(it) } } },
            )
            AppScreen.PLANS -> PlansScreen(Modifier.padding(padding), plans, onBack = { onScreen(AppScreen.HOME) }, onBuilder = { viewModel.clearBuilder(); onScreen(AppScreen.BUILDER) }, onPlan = { scope.launch { viewModel.preparePlan(it); onScreen(AppScreen.PREVIEW) } })
            AppScreen.BUILDER -> BuilderScreen(Modifier.padding(padding), exercises, builderIds, viewModel, onBack = { onScreen(AppScreen.PLANS) }, onPreview = { name -> viewModel.prepareManualPreview(name); onScreen(AppScreen.PREVIEW) })
            AppScreen.PREVIEW -> PreviewScreen(Modifier.padding(padding), previewTitle, preview, viewModel, onBack = { onScreen(AppScreen.PLANS) }, onStart = { scope.launch { viewModel.startPreparedSession()?.let(onActive) } })
            AppScreen.CATALOG -> CatalogScreen(Modifier.padding(padding), exercises, viewModel, onBack = { onScreen(AppScreen.ACTIVE) })
            AppScreen.ACTIVE -> ActiveWorkoutScreen(active, exercises, settings, viewModel, onFinish = { notes -> scope.launch { viewModel.finishSession(notes); onScreen(AppScreen.HOME) } }, onAdd = { onScreen(AppScreen.CATALOG) })
            AppScreen.HISTORY -> HistoryScreen(Modifier.padding(padding), recent, onBack = { onScreen(AppScreen.HOME) })
            AppScreen.SETTINGS -> SettingsScreen(Modifier.padding(padding), settings, viewModel, onBack = { onScreen(AppScreen.HOME) }, onAbout = { onScreen(AppScreen.ABOUT) })
            AppScreen.ABOUT -> AboutScreen(Modifier.padding(padding), updateState, viewModel, onBack = { onScreen(AppScreen.SETTINGS) })
        }
    }
    if (showUpdateDialog && updateState.release != null && screen != AppScreen.ABOUT) {
        val release = updateState.release
        if (release != null) UpdateAvailableDialog(release, onDismiss = { showUpdateDialog = false })
    }
}

@Composable
private fun Header(title: String, modifier: Modifier = Modifier, onBack: (() -> Unit)? = null, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
private fun HomeScreen(modifier: Modifier, plans: List<com.sol.gymtracker.data.db.PlanEntity>, recent: List<com.sol.gymtracker.data.db.WorkoutSessionEntity>, active: ActiveWorkout?, onPlans: () -> Unit, onSettings: () -> Unit, onStartPlan: (com.sol.gymtracker.data.db.PlanEntity) -> Unit, onResume: () -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Header("GymTracker", action = { IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, stringResource(R.string.action_settings)) } })
            val presets = remember(plans) { plans.filter { it.isPreset } }
            val nextPreset = presets.getOrNull(if (presets.isEmpty()) 0 else recent.size % presets.size)
            val nextCount = nextPreset?.let { presetExerciseCount(it.name) } ?: 0
            Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(20.dp)) {
                    if (nextPreset != null) {
                        Text(nextPreset.name.uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        if (nextCount > 0) pluralStringResource(R.plurals.exercises_count, nextCount, nextCount) else (nextPreset?.name ?: stringResource(R.string.home_presets_title)),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(stringResource(R.string.home_hero_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { if (nextPreset != null) onStartPlan(nextPreset) else onPlans() },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) { Text(stringResource(R.string.home_hero_start)) }
                    if (nextPreset != null) {
                        TextButton(onClick = onPlans, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.home_hero_choose)) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        if (active != null) item {
            Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(18.dp)) {
                    Text(stringResource(R.string.home_active_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        pluralStringResource(R.plurals.sets_of_progress, active.totalSets, active.completedSets, active.totalSets) + " · " + formatKgWithUnit(active.tonnage),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onResume, modifier = Modifier.fillMaxWidth().height(56.dp)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.home_continue)) }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(stringResource(R.string.home_presets_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.home_presets_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        items(plans.filter { it.isPreset }) { plan -> PlanQuickCard(plan, onStartPlan) }
        if (recent.isNotEmpty()) item {
            Text(stringResource(R.string.home_last_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp, 24.dp, 20.dp, 8.dp))
            val last = recent.first()
            val previous = recent.getOrNull(1)
            Text(
                stringResource(R.string.home_last_row, last.title, formatKg(last.totalTonnage), formatDurationRu(last.activeDurationSec)),
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (previous != null) {
                val delta = last.totalTonnage - previous.totalTonnage
                val deltaText = when {
                    delta > 0.5 -> stringResource(R.string.home_last_delta_up, formatKg(delta))
                    delta < -0.5 -> stringResource(R.string.home_last_delta_down, formatKg(-delta))
                    else -> stringResource(R.string.home_last_delta_same)
                }
                Text(deltaText, modifier = Modifier.padding(20.dp, 4.dp), color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun PlanQuickCard(plan: com.sol.gymtracker.data.db.PlanEntity, onStart: (com.sol.gymtracker.data.db.PlanEntity) -> Unit) {
    val accent = when (plan.name.lastOrNull()) {
        'A' -> Color(0xFFFFB86B)
        'B' -> Color(0xFF89D8FF)
        else -> Color(0xFFB99BFF)
    }
    Card(
        Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(6.dp).height(78.dp).clip(RoundedCornerShape(6.dp)).background(accent))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(plan.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    pluralStringResource(R.plurals.exercises_count, presetExerciseCount(plan.name), presetExerciseCount(plan.name)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.width(10.dp))
            Button(onClick = { onStart(plan) }, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(14.dp)) { Text(stringResource(R.string.home_hero_start)) }
        }
    }
}

private fun presetExerciseCount(name: String): Int = when (name.lastOrNull()) {
    'A', 'B' -> 6
    'C' -> 7
    else -> 0
}

@Composable
private fun PlansScreen(modifier: Modifier, plans: List<com.sol.gymtracker.data.db.PlanEntity>, onBack: () -> Unit, onBuilder: () -> Unit, onPlan: (com.sol.gymtracker.data.db.PlanEntity) -> Unit) {
    Column(modifier.fillMaxSize()) {
        Header(stringResource(R.string.plans_title), onBack = onBack, action = { IconButton(onClick = onBuilder) { Icon(Icons.Default.Add, stringResource(R.string.plans_create_action)) } })
        if (plans.isEmpty()) {
            EmptyState(stringResource(R.string.plans_empty))
            return@Column
        }
        LazyColumn(contentPadding = PaddingValues(16.dp)) { items(plans) { plan ->
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onPlan(plan) }) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (plan.isPreset) Icons.Default.FitnessCenter else Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(plan.name, fontWeight = FontWeight.Bold)
                        Text(
                            if (plan.isPreset) pluralStringResource(R.plurals.exercises_count, presetExerciseCount(plan.name), presetExerciseCount(plan.name))
                            else plan.description.ifBlank { stringResource(R.string.plans_custom_meta) },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(Icons.Default.ArrowUpward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } }
    }
}

@Composable
private fun BuilderScreen(modifier: Modifier, exercises: List<ExerciseEntity>, selected: List<String>, vm: GymViewModel, onBack: () -> Unit, onPreview: (String) -> Unit) {
    var group by rememberSaveable { mutableStateOf("Все") }
    var query by rememberSaveable { mutableStateOf("") }
    var planName by rememberSaveable { mutableStateOf("") }
    var showCustomDialog by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val planSavedMessage = stringResource(R.string.msg_plan_saved)
    val filtered = exercises.filter { (group == "Все" || it.muscleGroup == group) && (query.isBlank() || it.name.contains(query, true)) }
    Scaffold(modifier = modifier, topBar = { Header(stringResource(R.string.builder_title), onBack = onBack) }, bottomBar = {
        Surface(shadowElevation = 8.dp) { Column(Modifier.navigationBarsPadding().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showCustomDialog = true }, modifier = Modifier.weight(1f).height(52.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.builder_custom)) }
                OutlinedButton(onClick = { scope.launch { vm.saveBuilderPlan(planName); vm.showMessage(planSavedMessage) } }, enabled = selected.isNotEmpty(), modifier = Modifier.weight(1f).height(52.dp)) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(4.dp)); Text(stringResource(R.string.action_save)) }
            }
            Button(onClick = { onPreview(planName) }, enabled = selected.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(58.dp)) {
                Text(stringResource(R.string.builder_continue, pluralStringResource(R.plurals.exercises_count, selected.size, selected.size)))
            }
        } }
    }) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(planName, { planName = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), label = { Text(stringResource(R.string.builder_name_label)) }, placeholder = { Text(stringResource(R.string.builder_name_placeholder)) }, singleLine = true)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), label = { Text(stringResource(R.string.builder_search)) }, singleLine = true)
            GroupChips(group) { group = it }
            if (filtered.isEmpty()) {
                Text(stringResource(R.string.builder_nothing_found), modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 90.dp)) { items(filtered, key = { it.id }) { exercise -> ExercisePickRow(exercise, exercise.id in selected) { vm.toggleBuilderExercise(exercise.id) } } }
            }
        }
    }
    if (showCustomDialog) {
        CustomExerciseDialog(
            onDismiss = { showCustomDialog = false },
            onCreate = { name, muscle, equipment ->
                scope.launch {
                    vm.repository.createCustomExercise(name, muscle, equipment)
                    showCustomDialog = false
                    vm.showMessage(context.getString(R.string.msg_exercise_added))
                }
            },
        )
    }
}

@Composable
private fun PreviewScreen(modifier: Modifier, title: String, preview: List<PlannedExercise>, vm: GymViewModel, onBack: () -> Unit, onStart: () -> Unit) {
    Column(modifier.fillMaxSize()) {
        Header(stringResource(R.string.preview_title), onBack = onBack)
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
        Text(stringResource(R.string.preview_subtitle), modifier = Modifier.padding(20.dp, 4.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (preview.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth()) { EmptyState(stringResource(R.string.preview_empty)) }
        } else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp)) { itemsIndexed(preview) { index, item ->
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExerciseAccentLine(item.exercise.muscleGroup, Modifier.height(55.dp)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.exercise.name, fontWeight = FontWeight.Bold)
                        Text(
                            formatSetSummary(item.sets, item.repMin, item.repMax, item.restSeconds),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                    IconButton(onClick = { vm.removePreviewExercise(item.exercise.id) }) { Icon(Icons.Default.Delete, stringResource(R.string.action_remove)) }
                    Column {
                        IconButton(enabled = index > 0, onClick = { vm.movePreviewExercise(index, index - 1) }) { Icon(Icons.Default.ArrowUpward, stringResource(R.string.action_up)) }
                        IconButton(enabled = index < preview.lastIndex, onClick = { vm.movePreviewExercise(index, index + 1) }) { Icon(Icons.Default.ArrowDownward, stringResource(R.string.action_down)) }
                    }
                }
            }
        } }
        Button(onClick = onStart, enabled = preview.isNotEmpty(), modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(60.dp)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.preview_start)) }
    }
}

@Composable
private fun CatalogScreen(modifier: Modifier, exercises: List<ExerciseEntity>, vm: GymViewModel, onBack: () -> Unit) {
    var group by rememberSaveable { mutableStateOf("Все") }
    val active by vm.activeWorkout.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val filtered = exercises.filter { group == "Все" || it.muscleGroup == group }
    Column(modifier.fillMaxSize()) {
        Header(stringResource(R.string.catalog_title), onBack = onBack)
        GroupChips(group) { group = it }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) { items(filtered) { exercise -> ExercisePickRow(exercise, false) {
            active?.let { workout ->
                scope.launch { vm.addExerciseToActive(exercise); onBack() }
            }
        } } }
    }
}

@Composable
private fun CustomExerciseDialog(onDismiss: () -> Unit, onCreate: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("Ноги") }
    var equipment by remember { mutableStateOf("Тренажёр") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.custom_exercise_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.custom_exercise_name)) }, singleLine = true)
            OutlinedTextField(group, { group = it }, label = { Text(stringResource(R.string.custom_exercise_group)) }, singleLine = true)
            OutlinedTextField(equipment, { equipment = it }, label = { Text(stringResource(R.string.custom_exercise_equipment)) }, singleLine = true)
        } },
        confirmButton = { Button(onClick = { onCreate(name.trim(), group.trim(), equipment.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.custom_exercise_add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun HistoryScreen(modifier: Modifier, recent: List<com.sol.gymtracker.data.db.WorkoutSessionEntity>, onBack: () -> Unit) {
    Column(modifier.fillMaxSize()) {
        Header(stringResource(R.string.history_title), onBack = onBack)
        if (recent.isEmpty()) EmptyState(stringResource(R.string.history_empty)) else LazyColumn(contentPadding = PaddingValues(16.dp)) { items(recent) { session ->
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text(session.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        formatDateRu(session.startedAt) + " · " + formatDurationRu(session.activeDurationSec),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        formatKgWithUnit(session.totalTonnage),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (session.notes.isNotBlank()) {
                        Text(session.notes, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
        } }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier, settings: AppSettingsEntity, vm: GymViewModel, onBack: () -> Unit, onAbout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val exporter = ExportManager(context, vm.repository)
            val ok = exporter.write(uri, exporter.json())
            vm.showMessage(context.getString(if (ok) R.string.msg_copy_saved else R.string.msg_save_failed))
        }
    }
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            val exporter = ExportManager(context, vm.repository)
            val ok = exporter.write(uri, exporter.csv())
            vm.showMessage(context.getString(if (ok) R.string.msg_csv_saved else R.string.msg_save_failed))
        }
    }
    val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val exporter = ExportManager(context, vm.repository)
            exporter.writeInternalBackup(exporter.json())
            val payload = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } }.getOrNull()
            val restored = payload?.let { runCatching { vm.repository.restoreFromJson(it) }.getOrNull() } ?: 0
            vm.showMessage(if (restored > 0) context.getString(R.string.msg_imported, restored) else context.getString(R.string.msg_read_failed))
        }
    }
    Column(modifier.fillMaxSize()) {
        Header(stringResource(R.string.settings_title), onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
            item { SectionTitle(stringResource(R.string.settings_section_progression)) }
            item { StepSetting(settings, vm) }
            item { SectionTitle(stringResource(R.string.settings_section_data)) }
            item { SettingsAction(stringResource(R.string.settings_backup_title), stringResource(R.string.settings_backup_subtitle), Icons.Default.Backup) { jsonLauncher.launch("gymtracker-backup-${dateStamp()}.json") } }
            item { SettingsAction(stringResource(R.string.settings_csv_title), "gymtracker-${dateStamp()}.csv", Icons.Default.Upload) {
                csvLauncher.launch("gymtracker-${dateStamp()}.csv")
            } }
            item { SettingsAction(stringResource(R.string.settings_share_title), stringResource(R.string.settings_share_subtitle), Icons.Default.Upload) {
                scope.launch {
                    val exporter = ExportManager(context, vm.repository)
                    val uri = exporter.writeShareCache("gymtracker-backup-${dateStamp()}.json", exporter.json())
                    if (uri == null) vm.showMessage(context.getString(R.string.msg_share_failed)) else context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }, context.getString(R.string.settings_share_title)))
                }
            } }
            item { SettingsAction(stringResource(R.string.settings_import_title), stringResource(R.string.settings_import_subtitle), Icons.Default.Refresh) { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) } }
            item { SectionTitle(stringResource(R.string.drive_title)) }
            item { DriveSettings(settings, vm) }
            item { SectionTitle(stringResource(R.string.settings_section_app)) }
            item { SettingsAction(stringResource(R.string.settings_about_title), stringResource(R.string.settings_about_subtitle, BuildConfig.VERSION_NAME), Icons.Default.Info, onAbout) }
            item { SectionTitle(stringResource(R.string.settings_section_extra)) }
            item { Text(stringResource(R.string.settings_dev_hint), modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
        }
    }
}

private fun dateStamp(): String {
    val now = java.util.Calendar.getInstance()
    return "%04d-%02d-%02d".format(now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH) + 1, now.get(java.util.Calendar.DAY_OF_MONTH))
}

@Composable
private fun AboutScreen(modifier: Modifier, updateState: UpdateState, vm: GymViewModel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    Column(modifier.fillMaxSize()) {
        Header(stringResource(R.string.about_title), onBack = onBack)
        LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Card(Modifier.padding(16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(22.dp)) {
                        Text("GymTracker", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(stringResource(R.string.about_subtitle), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            item { SectionTitle(stringResource(R.string.about_section_author)) }
            item { SettingsAction(stringResource(R.string.about_author), stringResource(R.string.about_author_subtitle), Icons.Default.OpenInNew) { openUrl(context, GitHubReleaseChecker.profileUrl) } }
            item { SettingsAction(stringResource(R.string.about_source), stringResource(R.string.about_source_subtitle), Icons.Default.OpenInNew) { openUrl(context, GitHubReleaseChecker.repositoryUrl) } }
            item { SectionTitle(stringResource(R.string.about_section_updates)) }
            item {
                Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text("GitHub Releases", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when {
                                !GitHubReleaseChecker.isConfigured -> stringResource(R.string.settings_dev_hint)
                                updateState.isChecking -> stringResource(R.string.about_updates_checking)
                                updateState.release != null -> stringResource(R.string.update_dialog_title)
                                else -> stringResource(R.string.about_updates_none)
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        updateState.release?.let { latest ->
                            Spacer(Modifier.height(10.dp))
                            Text(latest.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                if (latest.isNewerThan(BuildConfig.VERSION_NAME)) stringResource(R.string.about_updates_available, latest.tagName)
                                else stringResource(R.string.about_updates_latest),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(enabled = !updateState.isChecking && GitHubReleaseChecker.isConfigured, onClick = { vm.checkForUpdates() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            NotificationIcon(Icons.Default.Refresh, updateState.release != null); Spacer(Modifier.width(8.dp))
                            Text(if (updateState.isChecking) stringResource(R.string.about_updates_checking) else stringResource(R.string.about_updates_check))
                        }
                        updateState.release?.let { latest ->
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { openUrl(context, latest.url) }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.about_updates_open)) }
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.about_updates_offline), modifier = Modifier.padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
        }
    }
}

@Composable
private fun StepSetting(settings: AppSettingsEntity, vm: GymViewModel) {
    var step by remember(settings.defaultStepKg) { mutableStateOf(settings.defaultStepKg) }
    Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
        Text(stringResource(R.string.settings_step_title, formatKg(step)), fontWeight = FontWeight.Bold)
        Slider(value = step.toFloat(), onValueChange = { step = (it * 10).roundToInt() / 10.0 }, valueRange = 0.1f..5f, steps = 48, onValueChangeFinished = { vm.updateSettings(settings.copy(defaultStepKg = step)) })
        Text(stringResource(R.string.settings_step_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    } }
}

@Composable
private fun DriveSettings(settings: AppSettingsEntity, vm: GymViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val driveManager = remember(context) { DriveSyncManager(context, vm.repository) }
    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val account = driveManager.accountFromResult(result.data)
        if (account == null) {
            vm.showMessage(context.getString(R.string.msg_google_failed))
        } else {
            scope.launch {
                val syncResult = runCatching { driveManager.sync(account) }.getOrElse { com.sol.gymtracker.sync.SyncResult(false, context.getString(R.string.msg_drive_offline)) }
                vm.showMessage(syncResult.message)
            }
        }
    }
    var enabled by remember(settings.syncEnabled) { mutableStateOf(settings.syncEnabled) }
    Card(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) { Column(Modifier.padding(18.dp)) {
        Text(stringResource(R.string.drive_title), fontWeight = FontWeight.Bold)
        Text(
            if (settings.driveAccount == null) stringResource(R.string.drive_not_connected) else stringResource(R.string.drive_connected, settings.driveAccount),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { driveManager.signInIntent()?.let(signInLauncher::launch) ?: vm.showMessage(context.getString(R.string.msg_google_not_configured)) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(stringResource(R.string.drive_connect)) }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.drive_auto_sync), Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = { value -> enabled = value; vm.updateSettings(settings.copy(syncEnabled = value)) })
        }
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick = { DriveSyncWorker.enqueue(context); vm.showMessage(context.getString(R.string.msg_sync_queued)) },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = settings.driveAccount != null,
        ) { Text(stringResource(R.string.drive_sync_now)) }
        Text(stringResource(R.string.drive_offline_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    } }
}

@Composable
private fun ActiveWorkoutScreen(active: ActiveWorkout?, exercises: List<ExerciseEntity>, settings: AppSettingsEntity, vm: GymViewModel, onFinish: (String) -> Unit, onAdd: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    var startedAt by remember(active?.session?.startedAt) { mutableLongStateOf(active?.session?.startedAt ?: System.currentTimeMillis()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var restEndsAt by remember { mutableLongStateOf(0L) }
    var showFinishDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(active?.session?.id) { activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) } }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    LaunchedEffect(restEndsAt) {
        if (restEndsAt > now) {
            delay((restEndsAt - now).coerceAtLeast(0L))
            vibrate(context)
            now = System.currentTimeMillis()
        }
    }
    val rest = ((restEndsAt - now).coerceAtLeast(0L) / 1000L).toInt()

    if (active == null) { EmptyState(stringResource(R.string.active_not_found)); return }
    Scaffold(topBar = { Header(active.session.title) }, bottomBar = {
        Column(Modifier.navigationBarsPadding()) {
            if (rest > 0) RestBar(rest, onMinus = { restEndsAt = (restEndsAt - 15_000L).coerceAtLeast(now) }, onPlus = { restEndsAt += 30_000L }, onSkip = { restEndsAt = 0L })
            Surface(shadowElevation = 10.dp) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAdd, Modifier.weight(1f).height(54.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(5.dp)); Text(stringResource(R.string.active_add)) }
                Button(onClick = { showFinishDialog = true }, Modifier.weight(1f).height(54.dp)) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(5.dp)); Text(stringResource(R.string.active_finish)) }
            } }
        }
    }) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 20.dp)) {
            item { WorkoutSummary(active, now - startedAt) }
            items(active.exercises, key = { it.sessionExercise.id }) { exercise -> ExerciseWorkoutCard(exercise, now, settings, vm, onRest = { restEndsAt = System.currentTimeMillis() + it * 1000L }) }
        }
    }
    if (showFinishDialog) {
        FinishWorkoutDialog(
            onDismiss = { showFinishDialog = false },
            onFinish = { notes -> showFinishDialog = false; onFinish(notes) },
        )
    }
}

@Composable
private fun FinishWorkoutDialog(onDismiss: () -> Unit, onFinish: (String) -> Unit) {
    var notes by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.active_finish_title)) },
        text = { OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.active_finish_notes)) }, placeholder = { Text(stringResource(R.string.active_finish_placeholder)) }, minLines = 2, maxLines = 4) },
        confirmButton = { Button(onClick = { onFinish(notes.trim()) }) { Text(stringResource(R.string.active_finish_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun WorkoutSummary(active: ActiveWorkout, elapsed: Long) {
    Card(Modifier.padding(16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primaryContainer)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(pluralStringResource(R.plurals.sets_of_progress, active.totalSets, active.completedSets, active.totalSets), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(formatKgWithUnit(active.tonnage), color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(formatTimer(elapsed / 1000), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    } }
}

@Composable
private fun ExerciseWorkoutCard(item: ActiveExercise, now: Long, settings: AppSettingsEntity, vm: GymViewModel, onRest: (Int) -> Unit) {
    var expanded by rememberSaveable(item.sessionExercise.id) { mutableStateOf(true) }
    Card(Modifier.padding(horizontal = 12.dp, vertical = 7.dp).fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                    ExerciseAccentLine(item.exercise.muscleGroup, Modifier.height(62.dp)); Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.exercise.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text(
                        stringResource(R.string.active_exercise_meta, item.exercise.muscleGroup, item.sessionExercise.repMin, item.sessionExercise.repMax, item.sessionExercise.targetRir),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
                if (item.isProgressionReady) Badge { Text(stringResource(R.string.active_progression_badge)) }
                IconButton(onClick = { expanded = !expanded }) { Icon(if (expanded) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, stringResource(R.string.action_down)) }
            }
            val previousText = previousSummaryOf(item.previousSets)
            if (previousText != null) {
                Text(stringResource(R.string.active_previous, previousText), color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            } else {
                Text(stringResource(R.string.active_previous_empty), color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
            }
            if (item.isProgressionReady) {
                Text(
                    stringResource(R.string.active_progression_hint, formatKg(item.exercise.defaultStepKg)),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Text(item.exercise.techniqueNote, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(top = 3.dp))
            if (expanded) item.sets.forEach { set -> SetRow(set, item.exercise, settings, vm, onRest) }
        }
    }
}

@Composable
private fun SetRow(set: LoggedSetEntity, exercise: ExerciseEntity, settings: AppSettingsEntity, vm: GymViewModel, onRest: (Int) -> Unit) {
    val scope = rememberCoroutineScope()
    var weight by remember(set.id, set.weightKg) { mutableStateOf(set.weightKg) }
    var reps by remember(set.id, set.reps) { mutableIntStateOf(set.reps) }
    var rir by remember(set.id, set.rir) { mutableIntStateOf(set.rir) }
    val step = when {
        settings.useMicroStep -> 0.1
        settings.defaultStepKg != 0.5 -> settings.defaultStepKg
        else -> exercise.defaultStepKg
    }
    Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(if (set.completed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.active_set_title, set.setIndex + 1, exercise.targetSets), fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    stringResource(if (set.completed) R.string.active_set_logged else R.string.active_log_set),
                    color = if (set.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                )
                Checkbox(set.completed, { checked -> scope.launch { vm.updateSet(set.copy(weightKg = weight, reps = reps, rir = rir), checked); if (checked) onRest(exercise.restSeconds) } })
            }
            Stepper(stringResource(R.string.active_weight), formatKgWithUnit(weight), { weight = (weight - step).coerceAtLeast(0.0); scope.launch { vm.updateSet(set.copy(weightKg = weight, reps = reps, rir = rir), set.completed) } }, { weight += step; scope.launch { vm.updateSet(set.copy(weightKg = weight, reps = reps, rir = rir), set.completed) } })
            Stepper(stringResource(R.string.active_reps), reps.toString(), { reps = (reps - 1).coerceAtLeast(0); scope.launch { vm.updateSet(set.copy(weightKg = weight, reps = reps, rir = rir), set.completed) } }, { reps += 1; scope.launch { vm.updateSet(set.copy(weightKg = weight, reps = reps, rir = rir), set.completed) } })
            Stepper(stringResource(R.string.active_rir), rir.toString(), { rir = (rir - 1).coerceAtLeast(0); scope.launch { vm.updateSet(set.copy(weightKg = weight, reps = reps, rir = rir), set.completed) } }, { rir += 1; scope.launch { vm.updateSet(set.copy(weightKg = weight, reps = reps, rir = rir), set.completed) } })
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, minus: () -> Unit, plus: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.width(76.dp), color = MaterialTheme.colorScheme.onSurfaceVariant); IconButton(onClick = minus, Modifier.size(52.dp)) { Text("−", fontSize = 26.sp) }; Text(value, Modifier.width(92.dp), fontWeight = FontWeight.Bold, fontSize = 17.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center); IconButton(onClick = plus, Modifier.size(52.dp)) { Text("+", fontSize = 26.sp) } }
}

@Composable
private fun RestBar(rest: Int, onMinus: () -> Unit, onPlus: () -> Unit, onSkip: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.tertiaryContainer), shape = RoundedCornerShape(0.dp)) { Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Timer, null); Spacer(Modifier.width(8.dp)); Text(formatTimer(rest.toLong()), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); TextButton(onClick = onMinus) { Text(stringResource(R.string.active_rest_minus)) }; TextButton(onClick = onPlus) { Text(stringResource(R.string.active_rest_plus)) }; TextButton(onClick = onSkip) { Text(stringResource(R.string.active_rest_skip)) } } }
}

@Composable
private fun GroupChips(selected: String, onSelect: (String) -> Unit) { Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Groups.forEach { group -> FilterChip(selected == group, { onSelect(group) }, label = { Text(group) }) } } }

@Composable
private fun ExercisePickRow(exercise: ExerciseEntity, selected: Boolean, onClick: () -> Unit) { Card(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { ExerciseAccentLine(exercise.muscleGroup, Modifier.height(54.dp)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(exercise.name, fontWeight = FontWeight.Bold); Text("${exercise.muscleGroup} · ${exercise.equipment}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Text(formatSetSummary(exercise.targetSets, exercise.repMin, exercise.repMax, exercise.restSeconds), color = MaterialTheme.colorScheme.secondary, fontSize = 12.sp) }; Icon(if (selected) Icons.Default.Check else Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary) } } }

@Composable
private fun ExerciseAccentLine(group: String, modifier: Modifier = Modifier) {
    val color = when (group) {
        "Ноги" -> Color(0xFFFFB86B)
        "Спина" -> Color(0xFF89D8FF)
        "Грудь" -> Color(0xFFFF86B7)
        "Плечи" -> Color(0xFFB99BFF)
        "Бицепс", "Трицепс" -> Color(0xFFD7FF4F)
        "Икры" -> Color(0xFFFFD166)
        "Пресс" -> Color(0xFF6EE7B7)
        else -> MaterialTheme.colorScheme.primary
    }
    Box(modifier.width(5.dp).clip(RoundedCornerShape(6.dp)).background(color))
}

@Composable
private fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp, 22.dp, 20.dp, 8.dp)) }

@Composable
private fun SettingsAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) { Card(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth().clickable(onClick = onClick)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.width(14.dp)); Column { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) } } } }

@Composable
private fun FilledActionButton(text: String, onClick: () -> Unit) { Button(onClick = onClick, contentPadding = PaddingValues(horizontal = 12.dp), modifier = Modifier.height(48.dp)) { Text(text) } }

@Composable
private fun EmptyState(text: String) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.FitnessCenter, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(10.dp)); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

private fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))) }
}

@Composable
private fun NotificationIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, hasNotification: Boolean) {
    Box(contentAlignment = Alignment.TopEnd) {
        Icon(icon, null)
        if (hasNotification) Box(Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(Color(0xFFFF5C6C)))
    }
}

@Composable
private fun UpdateAvailableDialog(release: GitHubRelease, onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_dialog_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(release.title, fontWeight = FontWeight.Bold); Text(stringResource(R.string.update_dialog_text, release.tagName)) } },
        confirmButton = { Button(onClick = { openUrl(context, release.url); onDismiss() }) { Text(stringResource(R.string.update_dialog_open)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_dialog_later)) } },
    )
}

private fun previousSummaryOf(sets: List<LoggedSetEntity>): String? =
    previousSetsSummary(sets.map { it.weightKg to it.reps }, if (sets.isEmpty()) 0 else sets.map { it.rir }.average().roundToInt())
@Suppress("DEPRECATION")
private fun vibrate(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    if (vibrator != null) {
        if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
        else vibrator.vibrate(250)
    }
    ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80).startTone(ToneGenerator.TONE_PROP_BEEP, 180)
}