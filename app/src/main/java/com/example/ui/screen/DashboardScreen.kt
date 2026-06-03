package com.example.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Workout
import com.example.data.model.WorkoutWithDetails
import com.example.ui.viewmodel.FitnessViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FitnessViewModel,
    onNavigateToLogger: (Long) -> Unit,
    onNavigateToLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val checkInDates by viewModel.checkInDates.collectAsState()
    val workoutDayStatuses by viewModel.workoutDayStatuses.collectAsState()
    val selectedDateWorkouts by viewModel.selectedDateWorkouts.collectAsState()
    val activeWorkout by viewModel.activeWorkout.collectAsState()

    var showCreatePlanDialog by remember { mutableStateOf(false) }

    // 锚定周，辅助日历翻页 (以周为单位增加或减少偏移)
    var currentWeekOffset by remember { mutableStateOf(0) }

    // 锚定基准时间戳
    val weekAnchorDate = remember(currentWeekOffset) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.WEEK_OF_YEAR, currentWeekOffset)
        cal.timeInMillis
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            text = "FitnessCheck",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "记录训练，重塑自我",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToLibrary,
                        modifier = Modifier.testTag("library_nav_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = "动作库",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            if (selectedDateWorkouts.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        showCreatePlanDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("start_workout_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新增计划"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "新增训练计划",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // 1. 周历头部月份与切换按纽
            WeekCalendarHeader(
                anchorDate = weekAnchorDate,
                onPreviousWeek = { currentWeekOffset-- },
                onNextWeek = { currentWeekOffset++ },
                onToday = {
                    currentWeekOffset = 0
                    viewModel.selectDate(System.currentTimeMillis())
                }
            )

            // 2. 横向周历卡片展示区域
            HorizontalWeekCalendar(
                anchorDate = weekAnchorDate,
                selectedDate = selectedDate,
                checkInDates = checkInDates,
                workoutDayStatuses = workoutDayStatuses,
                onDateSelected = { viewModel.selectDate(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. 选中日期的训练日志概览与动作列表
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (selectedDateWorkouts.isEmpty()) {
                    EmptyWorkoutState(
                        selectedDate = selectedDate,
                        onStartWorkout = {
                            showCreatePlanDialog = true
                        }
                    )
                } else {
                    WorkoutLogList(
                        workouts = selectedDateWorkouts,
                        onDeleteWorkout = { viewModel.deleteWorkoutRecord(it) },
                        onEditWorkout = { onNavigateToLogger(it.id) },
                        onStartWorkout = { workout ->
                            viewModel.startTraining(workout.id)
                            onNavigateToLogger(workout.id)
                        }
                    )
                }
            }
        }
    }

    if (showCreatePlanDialog) {
        CreatePlanDialog(
            onDismiss = { showCreatePlanDialog = false },
            onConfirm = { muscleGroup ->
                viewModel.createWorkoutPlan(selectedDate, muscleGroup)
                showCreatePlanDialog = false
            }
        )
    }
}

@Composable
fun WeekCalendarHeader(
    anchorDate: Long,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onToday: () -> Unit
) {
    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale.CHINA) }
    val monthYearStr = remember(anchorDate) { monthYearFormat.format(Date(anchorDate)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = monthYearStr,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = onToday,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("今天", fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onPreviousWeek) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "上一周"
                )
            }
            IconButton(onClick = onNextWeek) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "下一周"
                )
            }
        }
    }
}

@Composable
fun HorizontalWeekCalendar(
    anchorDate: Long,
    selectedDate: Long,
    checkInDates: Set<Long>,
    workoutDayStatuses: Map<Long, String> = emptyMap(),
    onDateSelected: (Long) -> Unit
) {
    // 根据 anchorDate 计算当周内星期一到星期日所有7天的毫秒值
    val daysOfWeek = remember(anchorDate) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = anchorDate
        // 设置到本周的第一天（我们按星期一作为第一天）
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // DAY_OF_WEEK 中，Sunday 是 1，Monday 是 2 ... Saturday 是 7
        val diffToMonday = if (dayOfWeek == Calendar.SUNDAY) -6 else 2 - dayOfWeek
        cal.add(Calendar.DAY_OF_YEAR, diffToMonday)

        val days = mutableListOf<Long>()
        for (i in 0 until 7) {
            val clone = cal.clone() as Calendar
            clone.set(Calendar.HOUR_OF_DAY, 0)
            clone.set(Calendar.MINUTE, 0)
            clone.set(Calendar.SECOND, 0)
            clone.set(Calendar.MILLISECOND, 0)
            days.add(clone.timeInMillis)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        days
    }

    val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
    val calInstance = Calendar.getInstance()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        daysOfWeek.forEachIndexed { index, dayMs ->
            calInstance.timeInMillis = dayMs
            val dayNum = calInstance.get(Calendar.DAY_OF_MONTH)

            // 是否为当前选中的日期
            val isSelected = remember(selectedDate, dayMs) {
                isSameDay(selectedDate, dayMs)
            }

            // 该日期是否完成了打卡 (即存在训练记录)
            val isCheckInDay = remember(checkInDates, dayMs) {
                checkInDates.contains(dayMs)
            }

            // 找出这个日期所属的训练状态
            val dayStatus = remember(workoutDayStatuses, dayMs) {
                workoutDayStatuses.entries.firstOrNull { isSameDay(it.key, dayMs) }?.value
            }

            // 是否为真正的今天
            val isToday = remember(dayMs) {
                isSameDay(System.currentTimeMillis(), dayMs)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 3.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isToday -> MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            else -> Color.Transparent
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }
                    )
                    .clickable { onDateSelected(dayMs) }
                    .padding(vertical = 10.dp)
                    .testTag("calendar_day_$dayNum"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayNames[index],
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = dayNum.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                        isToday -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 打卡星号/状态点指示器
                if (dayStatus != null) {
                    val dotColor = when (dayStatus) {
                        "IN_PROGRESS" -> Color(0xFF3B82F6) // 蓝色：训练中
                        "PLANNED" -> Color(0xFFF59E0B)     // 橙黄色：训练未开始
                        else -> Color(0xFF10B981)          // 绿色：训练已完成
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                } else if (isCheckInDay) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                } else {
                    Spacer(modifier = Modifier.size(7.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyWorkoutState(
    selectedDate: Long,
    onStartWorkout: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MM月dd日", Locale.CHINA) }
    val dateStr = dateFormat.format(Date(selectedDate))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.EventNote,
            contentDescription = "暂无日程",
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "$dateStr 还没有训练计划",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "动起来！为今天的优秀坚持打卡吧",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onStartWorkout,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.testTag("empty_start_button")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("新增训练计划")
        }
    }
}

@Composable
fun WorkoutLogList(
    workouts: List<WorkoutWithDetails>,
    onDeleteWorkout: (Workout) -> Unit,
    onEditWorkout: (Workout) -> Unit,
    onStartWorkout: (Workout) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("workout_log_list"),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 16.dp,
            end = 16.dp,
            bottom = 240.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "今日训练课表 (${workouts.size} 个记录)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        items(workouts) { workoutDetails ->
            WorkoutCompactCard(
                workoutDetails = workoutDetails,
                onDelete = { onDeleteWorkout(workoutDetails.workout) },
                onEdit = { onEditWorkout(workoutDetails.workout) },
                onStartWorkout = { onStartWorkout(workoutDetails.workout) }
            )
        }
    }
}

@Composable
fun WorkoutCompactCard(
    workoutDetails: WorkoutWithDetails,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onStartWorkout: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除计划?") },
            text = { Text("此操作将永久抹除该计划的所有组数和记录，且不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("删除", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    val status = workoutDetails.workout.status
    val totalSets = workoutDetails.sets.size
    val completedSets = workoutDetails.sets.count { it.workoutSet.isCompleted }
    val completionPercent = if (totalSets > 0) (completedSets * 100 / totalSets) else 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("workout_record_card_${workoutDetails.workout.id}"),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                "IN_PROGRESS" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                "COMPLETED" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (status == "IN_PROGRESS") BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 卡片头部：部位与状态提示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (workoutDetails.workout.targetMuscleGroup == "胸部") Icons.Default.FitnessCenter else Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = when (status) {
                            "IN_PROGRESS" -> Color(0xFF3B82F6)
                            "COMPLETED" -> Color(0xFF10B981)
                            else -> Color(0xFFF59E0B)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${workoutDetails.workout.targetMuscleGroup} 训练计划",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 状态提示 Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (status) {
                                "IN_PROGRESS" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                "COMPLETED" -> Color(0xFF10B981).copy(alpha = 0.15f)
                                else -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when (status) {
                            "IN_PROGRESS" -> "训练中 (${completionPercent}%)"
                            "COMPLETED" -> "训练完成"
                            else -> "未开始"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (status) {
                            "IN_PROGRESS" -> Color(0xFF2563EB)
                            "COMPLETED" -> Color(0xFF059669)
                            else -> Color(0xFFD97706)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 完成度情况与耗时
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "完成进度: $completedSets / $totalSets 组",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (status == "COMPLETED") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "耗时: ${formatDuration(workoutDetails.workout.duration)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 10.dp))

            // 动作与做组细节列表
            val groupedByExercise = remember(workoutDetails.sets) {
                workoutDetails.sets.groupBy { it.exercise }
            }

            if (groupedByExercise.isEmpty()) {
                Text(
                    text = "（待配置动作细节，请进入计划添加）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                groupedByExercise.forEach { (exercise, setsList) ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (status) {
                                            "IN_PROGRESS" -> Color(0xFF3B82F6)
                                            "COMPLETED" -> Color(0xFF10B981)
                                            else -> MaterialTheme.colorScheme.secondary
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = exercise.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Reps x Weight 组信息，如果是完成的状态，前面加一个 勾
                        val setsPreview = setsList.sortedBy { it.workoutSet.setNumber }.joinToString(" | ") { setEx ->
                            val checkPrefix = if (setEx.workoutSet.isCompleted) "✓ " else ""
                            "$checkPrefix${setEx.workoutSet.weight}kg × ${setEx.workoutSet.reps}次"
                        }

                        Text(
                            text = setsPreview,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 14.dp, top = 2.dp)
                        )
                    }
                }
            }

            // 心得体会总结
            if (workoutDetails.workout.note.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = "💡 心得: ${workoutDetails.workout.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 脚部操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "删除记录",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (status == "PLANNED") {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("配置计划", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onStartWorkout,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("start_workout_button_${workoutDetails.workout.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("开始运动", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (status) {
                                "IN_PROGRESS" -> Color(0xFF3B82F6)
                                "COMPLETED" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.primary
                            }
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        val label = when (status) {
                            "IN_PROGRESS" -> "继续录入"
                            "COMPLETED" -> "查看明细"
                            else -> "配置计划"
                        }
                        val iconVector = when (status) {
                            "IN_PROGRESS" -> Icons.Default.PlayArrow
                            "COMPLETED" -> Icons.Default.Check
                            else -> Icons.Default.PlayArrow
                        }
                        Icon(iconVector, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CreatePlanDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val muscleGroups = listOf("胸部", "背部", "腿部", "肩部", "手臂", "核心")
    var selectedGroup by remember { mutableStateOf(muscleGroups.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.EventNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("制定新训练计划", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "请选择本次训练的主要目标部位：",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    muscleGroups.chunked(3).forEach { rowGroups ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowGroups.forEach { group ->
                                val isSelected = selectedGroup == group
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedGroup = group },
                                    label = { Text(group, modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth(), textAlign = TextAlign.Center) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedGroup) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("保存计划")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// 辅助工具方法：秒数格式化为 hh:mm:ss 或 mm:ss
fun formatDuration(totalSeconds: Long): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60
    return if (hrs > 0) {
        String.format("%02d时%02d分%02d秒", hrs, mins, secs)
    } else {
        String.format("%02d分%02d秒", mins, secs)
    }
}

// 判断两个毫秒值是否归属于同一个历法自然天
fun isSameDay(ms1: Long, ms2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = ms1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = ms2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
