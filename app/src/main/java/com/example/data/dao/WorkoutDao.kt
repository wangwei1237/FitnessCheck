package com.example.data.dao

import androidx.room.*
import com.example.data.entity.Workout
import com.example.data.entity.WorkoutSet
import com.example.data.model.WorkoutWithDetails
import kotlinx.coroutines.flow.Flow

/**
 * 训练日记与组数记录数据库访问对象 (DAO)
 */
@Dao
interface WorkoutDao {

    // --- Workout 主表操作 ---

    @Transaction
    @Query("SELECT * FROM workouts ORDER BY date DESC")
    fun getAllWorkoutsWithDetails(): Flow<List<WorkoutWithDetails>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :workoutId LIMIT 1")
    fun getWorkoutWithDetailsById(workoutId: Long): Flow<WorkoutWithDetails?>

    @Query("SELECT * FROM workouts WHERE id = :workoutId LIMIT 1")
    suspend fun getWorkoutById(workoutId: Long): Workout?

    @Transaction
    @Query("SELECT * FROM workouts WHERE date >= :startMs AND date <= :endMs ORDER BY date DESC")
    fun getWorkoutsWithDetailsForDateRange(startMs: Long, endMs: Long): Flow<List<WorkoutWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: Workout): Long

    @Update
    suspend fun updateWorkout(workout: Workout)

    @Delete
    suspend fun deleteWorkout(workout: Workout)


    // --- WorkoutSet 子表操作 ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSet(workoutSet: WorkoutSet): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSets(workoutSets: List<WorkoutSet>)

    @Update
    suspend fun updateWorkoutSet(workoutSet: WorkoutSet)

    @Delete
    suspend fun deleteWorkoutSet(workoutSet: WorkoutSet)

    @Query("DELETE FROM workout_sets WHERE workoutId = :workoutId")
    suspend fun deleteSetsByWorkoutId(workoutId: Long)

    /**
     * 获取指定动作上一次做组的记录。
     * 关联 Workouts 表以确保按训练日期排序，而不仅仅是插入顺序
     */
    @Query("""
        SELECT ws.* FROM workout_sets ws 
        JOIN workouts w ON ws.workoutId = w.id 
        WHERE ws.exerciseId = :exerciseId 
        ORDER BY w.date DESC, ws.id DESC LIMIT 1
    """)
    suspend fun getLastSetForExercise(exerciseId: Long): WorkoutSet?

    /**
     * 获取当前训练会话中，指定动作的最后一组数据
     * 以便用户在当前训练中点击“复制上一组”时使用
     */
    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId AND exerciseId = :exerciseId ORDER BY setNumber DESC LIMIT 1")
    suspend fun getLastSetInWorkoutForExercise(workoutId: Long, exerciseId: Long): WorkoutSet?
}
