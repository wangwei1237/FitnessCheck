package com.example.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Exercise
import com.example.data.entity.WorkoutSet
import com.example.data.model.WorkoutSetWithExercise
import com.example.data.model.WorkoutWithDetails
import com.example.ui.viewmodel.FitnessViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLoggerScreen(
    viewModel: FitnessViewModel,
    workoutId: Long,
    onNavigateToSelectExercise: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 搭载及观察当前训练数据流
    LaunchedEffect(workoutId) {
        viewModel.loadWorkout(workoutId)
    }

    val activeWorkoutState by viewModel.activeWorkout.collectAsState()

    // 训练计时器，在进入页面或处于训练状态中，计算从开始点击按钮到当前为止的持续用时
    var secondsElapsed by remember { mutableStateOf(0L) }

    LaunchedEffect(activeWorkoutState) {
        val workout = activeWorkoutState?.workout
        if (workout != null) {
            if (workout.status == "COMPLETED") {
                secondsElapsed = workout.duration
            } else if (workout.status == "IN_PROGRESS" && workout.startTime > 0L) {
                secondsElapsed = (System.currentTimeMillis() - workout.startTime) / 1000
            }
        }
    }

    LaunchedEffect(activeWorkoutState?.workout?.status, activeWorkoutState?.workout?.startTime) {
        val workout = activeWorkoutState?.workout
        if (workout?.status == "IN_PROGRESS" && workout.startTime > 0L) {
            while (true) {
                secondsElapsed = (System.currentTimeMillis() - workout.startTime) / 1000
                delay(1000)
            }
        }
    }

    var noteText by remember { mutableStateOf("") }
    // 在 activeWorkout 加载完时塞入已保存的心得备注
    LaunchedEffect(activeWorkoutState) {
        activeWorkoutState?.let {
            if (noteText.isEmpty() && it.workout.note.isNotEmpty()) {
                noteText = it.workout.note
            }
        }
    }

    var showDiscardConfirm by remember { mutableStateOf(false) }
    var showFinishSummaryDialog by remember { mutableStateOf(false) }

    val status = activeWorkoutState?.workout?.status ?: "PLANNED"
    val isCompleted = status == "COMPLETED"
    val isInProgress = status == "IN_PROGRESS"
    val isPlanned = status == "PLANNED"

    var activeCountdownSeconds by remember { mutableStateOf(0) }
    var initialCountdownSeconds by remember { mutableStateOf(0) }
    var isTimerPaused by remember { mutableStateOf(false) }
    var activeCountdownSetId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(activeCountdownSeconds, isTimerPaused) {
        if (activeCountdownSeconds > 0 && !isTimerPaused) {
            delay(1000)
            activeCountdownSeconds--
        }
    }

    val totalSets = activeWorkoutState?.sets?.size ?: 0
    val completedSets = activeWorkoutState?.sets?.count { it.workoutSet.isCompleted } ?: 0
    val completionPercent = if (totalSets > 0) (completedSets * 100 / totalSets) else 0

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("要删除本次训练计划/记录吗？", fontWeight = FontWeight.Bold) },
            text = { Text("删除后，该计划以及当前录入的所有组数和数据都会被抹除，无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentWorkout = activeWorkoutState?.workout
                        if (currentWorkout != null) {
                            viewModel.deleteWorkoutRecord(currentWorkout)
                        }
                        viewModel.closeActiveWorkout()
                        showDiscardConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除计划", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 点击完成按钮后弹出的总结心得对话框
    if (showFinishSummaryDialog) {
        AlertDialog(
            onDismissRequest = { showFinishSummaryDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("今日训练完成！", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "恭喜你完成本次训练！本期指标课表如下：",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 呈现百分比 & 组数
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("总计划动作完成度", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("$completionPercent%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { completionPercent.toFloat() / 100f },
                                modifier = Modifier.fillMaxWidth().clip(CircleShape),
                                color = Color(0xFF10B981),
                                trackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "共完成 $completedSets 组（总计划 $totalSets 组）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("记录今日泵感或训练心得：", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = { Text("例：泵感极佳，推胸重量有所突破！", fontSize = 14.sp) },
                            modifier = Modifier.fillMaxWidth().height(90.dp).testTag("dialog_note_input"),
                            shape = RoundedCornerShape(8.dp),
                            maxLines = 3
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.finishWorkout(secondsElapsed, noteText)
                        viewModel.closeActiveWorkout()
                        showFinishSummaryDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("结束并打卡保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishSummaryDialog = false }) {
                    Text("返回微调")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        val titleLabel = when {
                            isPlanned -> "${activeWorkoutState?.workout?.targetMuscleGroup ?: ""} 计划中"
                            isCompleted -> "${activeWorkoutState?.workout?.targetMuscleGroup ?: ""} 训练明细"
                            else -> "训练中"
                        }
                        Text(titleLabel, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        if (!isPlanned) {
                            Text(
                                text = if (isCompleted) "耗时：${formatSeconds(secondsElapsed)}" else "持续用时：${formatSeconds(secondsElapsed)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isInProgress) Color(0xFF3B82F6) else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (isCompleted) {
                        IconButton(onClick = {
                            viewModel.closeActiveWorkout()
                            onNavigateBack()
                        }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else if (isPlanned) {
                        IconButton(onClick = {
                            showDiscardConfirm = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "取消并放弃",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                actions = {
                    if (!isCompleted) {
                        Button(
                            onClick = {
                                if (isPlanned) {
                                    viewModel.savePlannedWorkoutNote(noteText)
                                    onNavigateBack()
                                } else {
                                    showFinishSummaryDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("save_workout_button")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPlanned) "配置完成" else "完成训练", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(
                            onClick = {
                                viewModel.closeActiveWorkout()
                                onNavigateBack()
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("关闭", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        activeWorkoutState?.let { workoutWithDetails ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // 如果目前是“计划未开始 (PLANNED)”状态，我们在最顶上加一个引人注目的，可以一键开启的能量提示条！
                if (isPlanned) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .testTag("start_training_banner"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "🔥 准备好了吗？",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "该训练目前仅为日常计划，点击下方按钮开始今天的挑战！开启后将开始健身计时，且可以在做组之后进行完成打卡。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    viewModel.startTraining(workoutId)
                                    secondsElapsed = 0L // reset timer on start
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("start_training_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("立即开启本次打卡训练", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 1. 添加动作到计划的操作栏
                if (isPlanned) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "挑选并添加训练动作",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "为 ${workoutWithDetails.workout.targetMuscleGroup} 选配具体的训练项目",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { onNavigateToSelectExercise(workoutId) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("add_exercise_to_workout_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("动作库")
                            }
                        }
                    }
                }

                // 2. 核心列表：显示每个动作以及该动作底下的多个组(Sets)
                val groupedSets = remember(workoutWithDetails.sets) {
                    workoutWithDetails.sets.groupBy { it.exercise }
                }

                if (groupedSets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                              )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "计划中暂无动作",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "点击上方的“动作库”可以为你的计划添加动作、指定重量和做组次数！",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(groupedSets.keys.toList()) { exercise ->
                            val setsForThisExercise = groupedSets[exercise] ?: emptyList()
                            ExerciseSetsLoggerCard(
                                exercise = exercise,
                                sets = setsForThisExercise.map { it.workoutSet },
                                isEditable = !isCompleted,
                                onCopyLastSet = { viewModel.copyLastSetForExercise(exercise.id) },
                                onUpdateSet = { set, w, r, comp, rest ->
                                    if (!set.isCompleted && comp && isInProgress) {
                                        activeCountdownSeconds = rest
                                        initialCountdownSeconds = rest
                                        isTimerPaused = false
                                        activeCountdownSetId = set.id
                                    }
                                    viewModel.updateSetParameters(set, w, r, isCompleted = comp, restInterval = rest)
                                },
                                onDeleteSet = { set -> viewModel.deleteSet(set) }
                            )
                        }
                    }
                }

                // 3. 底部心得输入/展示区域
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.RateReview,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "训练心得记录",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (!isCompleted) {
                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                placeholder = { Text("记录今天感觉如何、肌肉泵感、有什么需要改进的...", fontSize = 14.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("workout_note_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else {
                            Text(
                                text = if (noteText.isNotEmpty()) noteText else "本节训练未录入具体心得体会。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        
        }

        // 组间休息自动倒计时悬浮卡片
        if (activeCountdownSeconds > 0) {
            RestCountdownBanner(
                remainingSeconds = activeCountdownSeconds,
                totalSeconds = initialCountdownSeconds,
                isPaused = isTimerPaused,
                onTogglePause = { isTimerPaused = !isTimerPaused },
                onAddTenSeconds = {
                    activeCountdownSeconds += 10
                    initialCountdownSeconds += 10
                    activeCountdownSetId?.let { setId ->
                        activeWorkoutState?.sets?.find { it.workoutSet.id == setId }?.let { pair ->
                            val updatedRest = pair.workoutSet.restInterval + 10
                            viewModel.updateSetParameters(
                                set = pair.workoutSet,
                                weight = pair.workoutSet.weight,
                                reps = pair.workoutSet.reps,
                                isCompleted = pair.workoutSet.isCompleted,
                                restInterval = updatedRest
                            )
                        }
                    }
                },
                onSkip = { activeCountdownSeconds = 0 },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 96.dp)
            )
        }
    }
}

@Composable
fun ExerciseSetsLoggerCard(
    exercise: Exercise,
    sets: List<WorkoutSet>,
    isEditable: Boolean,
    onCopyLastSet: () -> Unit,
    onUpdateSet: (WorkoutSet, Double, Int, Boolean, Int) -> Unit,
    onDeleteSet: (WorkoutSet) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exercise_log_card_${exercise.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
            )
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 动作卡片头部：名称、肌肉分类、右侧操作
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = exercise.muscleGroup,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 快速追加做组（在可修改的情况下）
                if (isEditable) {
                    TextButton(
                        onClick = onCopyLastSet,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("copy_last_set_btn_${exercise.id}")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("新增/复制上组", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 列表头标识
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(36.dp)) // 预留给 check 图标的大小对齐
                Text(
                    "组号",
                    modifier = Modifier.width(36.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    "重量 (粗略)",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    "次数 (完成度)",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (isEditable) {
                    Spacer(modifier = Modifier.size(36.dp)) // 预留给 trash 垃圾箱
                }
            }

            // 循环多组细节输入/展示行
            val sortedSets = remember(sets) {
                sets.sortedBy { it.setNumber }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                sortedSets.forEach { set ->
                    SetRecordingRow(
                        set = set,
                        onUpdateWeight = { delta ->
                            val newWeight = (set.weight + delta).coerceAtLeast(0.0)
                            onUpdateSet(set, newWeight, set.reps, set.isCompleted, set.restInterval)
                        },
                        onUpdateReps = { delta ->
                            val newReps = (set.reps + delta).coerceAtLeast(0)
                            onUpdateSet(set, set.weight, newReps, set.isCompleted, set.restInterval)
                        },
                        onToggleCompleted = {
                            onUpdateSet(set, set.weight, set.reps, !set.isCompleted, set.restInterval)
                        },
                        onUpdateRest = { delta ->
                            val newRest = (set.restInterval + delta).coerceAtLeast(10)
                            onUpdateSet(set, set.weight, set.reps, set.isCompleted, newRest)
                        },
                        onDelete = { onDeleteSet(set) },
                        canEdit = isEditable
                    )
                }
            }
        }
    }
}

@Composable
fun SetRecordingRow(
    set: WorkoutSet,
    onUpdateWeight: (Double) -> Unit,
    onUpdateReps: (Int) -> Unit,
    onToggleCompleted: () -> Unit,
    onUpdateRest: (Int) -> Unit,
    onDelete: () -> Unit,
    canEdit: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (set.isCompleted) Color(0xFF10B981).copy(alpha = 0.08f) else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // A. 漂亮圆圈一键标记勾选完成
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            width = 2.dp,
                            color = if (set.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .background(
                            color = if (set.isCompleted) Color(0xFF10B981) else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable(enabled = canEdit) { onToggleCompleted() },
                    contentAlignment = Alignment.Center
                ) {
                    if (set.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            // B. 组号
            Box(
                modifier = Modifier.width(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = set.setNumber.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (set.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                )
            }

            // C. 重量操纵器
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canEdit) {
                    IconButton(
                        onClick = { onUpdateWeight(-2.5) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "减少重量",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${set.weight}kg",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (set.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                if (canEdit) {
                    IconButton(
                        onClick = { onUpdateWeight(2.5) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "增加重量",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // D. 次数操纵器
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canEdit) {
                    IconButton(
                        onClick = { onUpdateReps(-1) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "减少次数",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "${set.reps}下",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (set.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                if (canEdit) {
                    IconButton(
                        onClick = { onUpdateReps(1) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "增加次数",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // E. 删除这组
            if (canEdit) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除该组",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Sub-row representing scheduled Rest Interval during training configuration or display
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 72.dp, top = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (set.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "组间休息: ",
                style = MaterialTheme.typography.labelSmall,
                color = if (set.isCompleted) Color(0xFF10B981).copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (canEdit) {
                IconButton(
                    onClick = { onUpdateRest(-10) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "减少组间休息时间",
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "${set.restInterval}秒",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { onUpdateRest(10) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "增加组间休息时间",
                        modifier = Modifier.size(10.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "${set.restInterval}秒",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (set.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// 辅助格式化时间
fun formatSeconds(totalSeconds: Long): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

@Composable
fun RestCountdownBanner(
    remainingSeconds: Int,
    totalSeconds: Int,
    isPaused: Boolean,
    onTogglePause: () -> Unit,
    onAddTenSeconds: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .shadow(12.dp, shape = RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Timer progress visualization
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(44.dp)
            ) {
                CircularProgressIndicator(
                    progress = { if (totalSeconds > 0) remainingSeconds.toFloat() / totalSeconds.toFloat() else 0f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                    strokeWidth = 3.dp
                )
                Text(
                    text = "${remainingSeconds}s",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Info guide labels
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🔋 组间自动休息中...",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "深呼吸，准备下一组挑战",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = onTogglePause,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = "暂停/继续",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }

                TextButton(
                    onClick = onAddTenSeconds,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("+10s", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = onSkip,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("跳过", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
