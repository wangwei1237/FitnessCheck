package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.entity.Exercise
import com.example.data.entity.Workout
import com.example.data.entity.WorkoutSet
import com.example.data.model.WorkoutWithDetails
import com.example.data.repository.FitnessRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * FitnessCheck 健身核心控制中心 ViewModel
 *
 * 采用全 Flow 的声明式流（Declarative reactive programming）开发：
 * 具备响应日历切换、自动高亮打卡日期、训练本草录入、快速复制上组数据等多核逻辑。
 */
class FitnessViewModel(private val repository: FitnessRepository) : ViewModel() {

    // ==========================================
    // 1. 日历打卡及首页状态相关
    // ==========================================

    // 选中日期的毫秒戳，默认初始化为当前系统毫秒
    private val _selectedDate = MutableStateFlow(System.currentTimeMillis())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    // 动作库数据列表
    val allExercises: StateFlow<List<Exercise>> = repository.allExercises
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 所有有过训练打卡的毫秒集合，用在日历中高亮展示
    val checkInDates: StateFlow<Set<Long>> = repository.allWorkoutsWithDetails
        .map { workoutList ->
            workoutList.map { truncateToStartOfDay(it.workout.date) }.toSet()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet()
        )

    // 每一个有训练计划或历史打卡的日期状态映射（训练中 > 训练未开始 > 训练结束）
    val workoutDayStatuses: StateFlow<Map<Long, String>> = repository.allWorkoutsWithDetails
        .map { workoutList ->
            val statusMap = mutableMapOf<Long, String>()
            val grouped = workoutList.groupBy { truncateToStartOfDay(it.workout.date) }
            grouped.forEach { (dayMs, list) ->
                val statuses = list.map { it.workout.status }
                statusMap[dayMs] = when {
                    statuses.contains("IN_PROGRESS") -> "IN_PROGRESS"
                    statuses.contains("PLANNED") -> "PLANNED"
                    else -> "COMPLETED"
                }
            }
            statusMap
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    // 选中日期的训练日志。使用 Combine 算子响应 selectedDate 或 历史数据 的任何变化并自动刷新过滤
    val selectedDateWorkouts: StateFlow<List<WorkoutWithDetails>> = combine(
        selectedDate,
        repository.allWorkoutsWithDetails
    ) { dateMs, workouts ->
        val targetStart = truncateToStartOfDay(dateMs)
        val targetEnd = targetStart + 24 * 60 * 60 * 1000 - 1

        workouts.filter {
            it.workout.date in targetStart..targetEnd
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    // ==========================================
    // 2. 当前正在记录的训练会话状态 (Workout Logger Screen)
    // ==========================================

    // 当前正在进行的 Workout 档案，若无进行中默认为 null
    private val _activeWorkout = MutableStateFlow<WorkoutWithDetails?>(null)
    val activeWorkout: StateFlow<WorkoutWithDetails?> = _activeWorkout.asStateFlow()

    // 按肌肉群筛选动作的流
    private val _selectedMuscleGroup = MutableStateFlow("全部")
    val selectedMuscleGroup: StateFlow<String> = _selectedMuscleGroup.asStateFlow()

    val filteredExercisesToLog: StateFlow<List<Exercise>> = combine(
        _selectedMuscleGroup,
        allExercises
    ) { group, exercises ->
        if (group == "全部") {
            exercises
        } else {
            exercises.filter { it.muscleGroup == group }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    init {
        // 完成实例化时自动唤起初始化动作库植入逻辑，规避阻断
        viewModelScope.launch {
            repository.seedExercisesIfEmpty()
        }
    }

    // ==========================================
    // 3. 业务动作 (Business Intents / Actions)
    // ==========================================

    /**
     * 切换日历上的选中日期
     */
    fun selectDate(timestampMillisecond: Long) {
        _selectedDate.value = timestampMillisecond
    }

    /**
     * 开始一次全新训练
     */
    fun startNewWorkout(dateMs: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val newWorkout = Workout(
                date = dateMs,
                note = "",
                status = "IN_PROGRESS"
            )
            val insertedId = repository.insertWorkout(newWorkout)

            // 初始化一个包含此 workout 主表的复合细节
            _activeWorkout.value = WorkoutWithDetails(
                workout = Workout(id = insertedId, date = dateMs, status = "IN_PROGRESS"),
                sets = emptyList()
            )

            // 在每次修改时都可以通过订阅单个 Workout 流来维持 activeWorkout 数据在本地同步更新
            observeActiveWorkout(insertedId)
        }
    }

    /**
     * 创建训练计划
     */
    fun createWorkoutPlan(dateMs: Long, targetMuscleGroup: String) {
        viewModelScope.launch {
            val newWorkout = Workout(
                date = dateMs,
                note = "",
                status = "PLANNED",
                targetMuscleGroup = targetMuscleGroup
            )
            repository.insertWorkout(newWorkout)
        }
    }

    /**
     * 加载当前训练/计划
     */
    fun loadWorkout(workoutId: Long) {
        observeActiveWorkout(workoutId)
    }

    /**
     * 开始训练
     */
    fun startTraining(workoutId: Long) {
        viewModelScope.launch {
            val workout = repository.getWorkoutById(workoutId)
            workout?.let { w ->
                val updatedWorkout = w.copy(
                    status = "IN_PROGRESS",
                    startTime = System.currentTimeMillis()
                )
                repository.updateWorkout(updatedWorkout)
                _activeWorkout.value?.let { activeW ->
                    if (activeW.workout.id == workoutId) {
                        _activeWorkout.value = activeW.copy(workout = updatedWorkout)
                    }
                }
            }
        }
    }

    /**
     * 订阅监听当前训练记录的动态变更，以保证 activeWorkout 高度实时
     */
    private fun observeActiveWorkout(workoutId: Long) {
        viewModelScope.launch {
            repository.getWorkoutWithDetailsById(workoutId).collect { workoutDetails ->
                _activeWorkout.value = workoutDetails
            }
        }
    }

    /**
     * 放弃或保存完工，关闭当前训练控制台
     */
    fun closeActiveWorkout() {
        _activeWorkout.value = null
    }

    /**
     * 为当前训练添加指定的运动训练动作，并在第一组植入
     */
    fun addExerciseToActiveWorkout(exerciseId: Long) {
        val activeW = _activeWorkout.value ?: return
        viewModelScope.launch {
            // 通过获取上次历史训练中的做组参数实现首组填充
            val lastHistoricalSet = repository.getLastSetForExercise(exerciseId)
            val defaultWeight = lastHistoricalSet?.weight ?: 20.0
            val defaultReps = lastHistoricalSet?.reps ?: 10

            val firstSet = WorkoutSet(
                workoutId = activeW.workout.id,
                exerciseId = exerciseId,
                setNumber = 1,
                weight = defaultWeight,
                reps = defaultReps
            )
            repository.insertWorkoutSet(firstSet)
        }
    }

    /**
     * 快速复制上一组（智能复刻功能）
     * 功能原理：若该动作在当前训练已存在组数，则复制该组在当前会话的最新一组的参数，组号递增。
     * 若当前会话是首次添加，则从历史数据中拉取最新一条参数。
     */
    fun copyLastSetForExercise(exerciseId: Long) {
        val activeW = _activeWorkout.value ?: return
        viewModelScope.launch {
            // 首先查找当前本次训练中该动作最新完成的那组数据
            val lastSetInSession = repository.getLastSetInWorkoutForExercise(activeW.workout.id, exerciseId)
            // 如果不存在，继续回溯查找历史最接近的一条做组记录
            val targetSourceSet = lastSetInSession ?: repository.getLastSetForExercise(exerciseId)

            val currentSetsCount = activeW.sets.filter { it.workoutSet.exerciseId == exerciseId }.size
            val nextSetNum = currentSetsCount + 1

            val newSet = WorkoutSet(
                workoutId = activeW.workout.id,
                exerciseId = exerciseId,
                setNumber = nextSetNum,
                weight = targetSourceSet?.weight ?: 20.0,
                reps = targetSourceSet?.reps ?: 10
            )
            repository.insertWorkoutSet(newSet)
        }
    }

    /**
     * 修改组数据详情（即席增减重量、数量、完成状态、休眠时间）
     */
    fun updateSetParameters(
        set: WorkoutSet,
        weight: Double,
        reps: Int,
        isCompleted: Boolean = set.isCompleted,
        restInterval: Int = set.restInterval
    ) {
        viewModelScope.launch {
            repository.updateWorkoutSet(
                set.copy(
                    weight = weight,
                    reps = reps,
                    isCompleted = isCompleted,
                    restInterval = restInterval
                )
            )
        }
    }

    /**
     * 删除指定组 (Set)
     */
    fun deleteSet(set: WorkoutSet) {
        viewModelScope.launch {
            repository.deleteWorkoutSet(set)
            
            // 重新整理组序号
            val activeW = _activeWorkout.value ?: return@launch
            val sameExerciseSets = activeW.sets
                .filter { it.workoutSet.exerciseId == set.exerciseId && it.workoutSet.id != set.id }
                .sortedBy { it.workoutSet.setNumber }
            
            sameExerciseSets.forEachIndexed { index, pair ->
                val updatedNum = index + 1
                if (pair.workoutSet.setNumber != updatedNum) {
                    repository.updateWorkoutSet(pair.workoutSet.copy(setNumber = updatedNum, isCompleted = pair.workoutSet.isCompleted, restInterval = pair.workoutSet.restInterval))
                }
            }
        }
    }

    /**
     * 更新当前训练的总训练时间与备注心得。代表最终保存提交训练
     */
    fun finishWorkout(durationSeconds: Long, noteText: String) {
        val activeW = _activeWorkout.value ?: return
        viewModelScope.launch {
            repository.updateWorkout(
                activeW.workout.copy(
                    duration = durationSeconds,
                    note = noteText,
                    status = "COMPLETED"
                )
            )
            closeActiveWorkout()
        }
    }

    /**
     * 仅保存训练计划的心得备注或动作配置，保持 PLANNED 状态不变
     */
    fun savePlannedWorkoutNote(noteText: String) {
        val activeW = _activeWorkout.value ?: return
        viewModelScope.launch {
            repository.updateWorkout(
                activeW.workout.copy(
                    note = noteText
                )
            )
            closeActiveWorkout()
        }
    }

    /**
     * 删除单次训练记录
     */
    fun deleteWorkoutRecord(workout: Workout) {
        viewModelScope.launch {
            repository.deleteWorkout(workout)
        }
    }

    // ==========================================
    // 4. 自定义动作库交互流
    // ==========================================

    fun selectMuscleGroup(group: String) {
        _selectedMuscleGroup.value = group
    }

    fun addNewCustomExercise(name: String, muscleGroup: String) {
        viewModelScope.launch {
            val customEx = Exercise(
                name = name,
                muscleGroup = muscleGroup,
                isCustom = true
            )
            repository.insertExercise(customEx)
        }
    }


    // ==========================================
    // 5. 内部时间戳格式化辅助器
    // ==========================================

    private fun truncateToStartOfDay(timeMs: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timeMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}

/**
 * 带有参数的 ViewModelFactory 声明助手
 */
class FitnessViewModelFactory(private val repository: FitnessRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FitnessViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FitnessViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
