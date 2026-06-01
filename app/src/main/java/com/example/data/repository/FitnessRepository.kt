package com.example.data.repository

import com.example.data.dao.ExerciseDao
import com.example.data.dao.WorkoutDao
import com.example.data.entity.Exercise
import com.example.data.entity.Workout
import com.example.data.entity.WorkoutSet
import com.example.data.model.WorkoutWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * 健身记录仓储层 (Repository Pattern)
 * 封装数据访问逻辑，对上层 (ViewModel) 屏蔽具体的数据存储细节
 */
class FitnessRepository(
    private val exerciseDao: ExerciseDao,
    private val workoutDao: WorkoutDao
) {

    // ==========================================
    // 1. 动作库相关流程 (Exercise Flow)
    // ==========================================

    /**
     * 获取完整的动作列表（流式响应）
     */
    val allExercises: Flow<List<Exercise>> = exerciseDao.getAllExercises()

    /**
     * 按照肌肉群分类筛选动作
     */
    fun getExercisesByMuscleGroup(muscleGroup: String): Flow<List<Exercise>> {
        return exerciseDao.getExercisesByMuscleGroup(muscleGroup)
    }

    /**
     * 写入新训练动作（支持用户自定义）
     */
    suspend fun insertExercise(exercise: Exercise): Long {
        return exerciseDao.insertExercise(exercise)
    }

    /**
     * 删除特定的动作
     */
    suspend fun deleteExercise(exercise: Exercise) {
        exerciseDao.deleteExercise(exercise)
    }

    /**
     * 经典内置训练动作自动初始化逻辑
     * 若检测到本地动作为空，则主动植入多条常用基础训练动作，极大地提升了新用户的首发体验
     */
    suspend fun seedExercisesIfEmpty() {
        val currentList = exerciseDao.getAllExercises().first()
        if (currentList.isEmpty()) {
            val defaults = listOf(
                // 胸部 (Chest)
                Exercise(name = "杠铃平板卧推", muscleGroup = "胸部", isCustom = false),
                Exercise(name = "哑铃上斜推举", muscleGroup = "胸部", isCustom = false),
                Exercise(name = "哑铃下斜飞鸟", muscleGroup = "胸部", isCustom = false),
                Exercise(name = "双杠臂屈伸", muscleGroup = "胸部", isCustom = false),
                Exercise(name = "绳索夹胸夹下部", muscleGroup = "胸部", isCustom = false),

                // 背部 (Back)
                Exercise(name = "引体向上", muscleGroup = "背部", isCustom = false),
                Exercise(name = "高位下拉", muscleGroup = "背部", isCustom = false),
                Exercise(name = "杠铃俯身划船", muscleGroup = "背部", isCustom = false),
                Exercise(name = "哑铃单臂划船", muscleGroup = "背部", isCustom = false),
                Exercise(name = "坐姿宽距划船", muscleGroup = "背部", isCustom = false),

                // 腿部 (Legs)
                Exercise(name = "杠铃背部深蹲", muscleGroup = "腿部", isCustom = false),
                Exercise(name = "罗马尼亚硬拉", muscleGroup = "腿部", isCustom = false),
                Exercise(name = "哑铃保加利亚剪步蹲", muscleGroup = "腿部", isCustom = false),
                Exercise(name = "坐姿腿屈伸", muscleGroup = "腿部", isCustom = false),
                Exercise(name = "俯卧腿弯举", muscleGroup = "腿部", isCustom = false),

                // 肩部 (Shoulders)
                Exercise(name = "杠铃站姿过顶推举", muscleGroup = "肩部", isCustom = false),
                Exercise(name = "哑铃坐姿推举", muscleGroup = "肩部", isCustom = false),
                Exercise(name = "哑铃侧平举", muscleGroup = "肩部", isCustom = false),
                Exercise(name = "哑铃俯身飞鸟", muscleGroup = "肩部", isCustom = false),

                // 手臂 (Arms)
                Exercise(name = "哑铃交替弯举", muscleGroup = "手臂", isCustom = false),
                Exercise(name = "杠铃牧师凳弯举", muscleGroup = "手臂", isCustom = false),
                Exercise(name = "绳索直臂下压", muscleGroup = "手臂", isCustom = false),
                Exercise(name = "哑铃人面颈后臂屈伸", muscleGroup = "手臂", isCustom = false),

                // 核心/腹肌 (Core)
                Exercise(name = "仰卧卷腹", muscleGroup = "核心", isCustom = false),
                Exercise(name = "平板支撑", muscleGroup = "核心", isCustom = false),
                Exercise(name = "悬挂抬腿", muscleGroup = "核心", isCustom = false)
            )
            exerciseDao.insertDefaultExercises(defaults)
        }
    }


    // ==========================================
    // 2. 训练记录相关流程 (Workout Flow)
    // ==========================================

    /**
     * 获取历史上所有的训练日志（带有完整做组明细的流）
     */
    val allWorkoutsWithDetails: Flow<List<WorkoutWithDetails>> = workoutDao.getAllWorkoutsWithDetails()

    /**
     * 根据特定 ID 获取对应训练日志详细流
     */
    fun getWorkoutWithDetailsById(workoutId: Long): Flow<WorkoutWithDetails?> {
        return workoutDao.getWorkoutWithDetailsById(workoutId)
    }

    /**
     * 根据特定 ID 直接获取 Workout 实体
     */
    suspend fun getWorkoutById(workoutId: Long): Workout? {
        return workoutDao.getWorkoutById(workoutId)
    }

    /**
     * 获取指定时间戳范围内的训练流水明细，用于日历视图按选定年份或月份提取明细
     */
    fun getWorkoutsWithDetailsForDateRange(startMs: Long, endMs: Long): Flow<List<WorkoutWithDetails>> {
        return workoutDao.getWorkoutsWithDetailsForDateRange(startMs, endMs)
    }

    /**
     * 创建一次新的训练会话
     */
    suspend fun insertWorkout(workout: Workout): Long {
        return workoutDao.insertWorkout(workout)
    }

    /**
     * 更新某次训练的主信息（如备注或时长等）
     */
    suspend fun updateWorkout(workout: Workout) {
        workoutDao.updateWorkout(workout)
    }

    /**
     * 删除单次训练（由于设置了级联，其关联的组数据也将一并自动清理）
     */
    suspend fun deleteWorkout(workout: Workout) {
        workoutDao.deleteWorkout(workout)
    }


    // ==========================================
    // 3. 做组数据相关流程 (WorkoutSet Flow)
    // ==========================================

    /**
     * 记录一组训练数据
     */
    suspend fun insertWorkoutSet(workoutSet: WorkoutSet): Long {
        return workoutDao.insertWorkoutSet(workoutSet)
    }

    /**
     * 批量导入做组数据
     */
    suspend fun insertWorkoutSets(sets: List<WorkoutSet>) {
        workoutDao.insertWorkoutSets(sets)
    }

    /**
     * 修改某一组记录（修正其重量或次数）
     */
    suspend fun updateWorkoutSet(workoutSet: WorkoutSet) {
        workoutDao.updateWorkoutSet(workoutSet)
    }

    /**
     * 删除指定次数的组数据
     */
    suspend fun deleteWorkoutSet(workoutSet: WorkoutSet) {
        workoutDao.deleteWorkoutSet(workoutSet)
    }

    /**
     * 清理单次训练下的所有打卡组信息
     */
    suspend fun deleteSetsByWorkoutId(workoutId: Long) {
        workoutDao.deleteSetsByWorkoutId(workoutId)
    }

    /**
     * 快速复刻功能一：获取指定动作的前序最后一次训练组记录
     */
    suspend fun getLastSetForExercise(exerciseId: Long): WorkoutSet? {
        return workoutDao.getLastSetForExercise(exerciseId)
    }

    /**
     * 快速复刻功能二：获取当前会话中，指派动作的上一组数据
     */
    suspend fun getLastSetInWorkoutForExercise(workoutId: Long, exerciseId: Long): WorkoutSet? {
        return workoutDao.getLastSetInWorkoutForExercise(workoutId, exerciseId)
    }
}
