package com.example.data.dao

import androidx.room.*
import com.example.data.entity.Exercise
import kotlinx.coroutines.flow.Flow

/**
 * 训练动作数据库访问对象 (DAO)
 */
@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY id ASC")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup ORDER BY name ASC")
    fun getExercisesByMuscleGroup(muscleGroup: String): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getExerciseById(id: Long): Exercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: Exercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefaultExercises(exercises: List<Exercise>)

    @Delete
    suspend fun deleteExercise(exercise: Exercise)
}
