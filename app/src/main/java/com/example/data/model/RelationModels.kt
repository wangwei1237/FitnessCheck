package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.entity.Exercise
import com.example.data.entity.Workout
import com.example.data.entity.WorkoutSet

/**
 * 训练组数据与其对应动作组合的信息
 * 利用 Room 的 @Relation 实现优雅的关联查询，从而在展示组数据时直接访问动作名字和肌肉部位
 */
data class WorkoutSetWithExercise(
    @Embedded
    val workoutSet: WorkoutSet,

    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: Exercise
)

/**
 * 一个完整的训练日志
 * 包含训练主信息（如日期、心得等）以及该次训练持有的所有动作、组数数据列表
 */
data class WorkoutWithDetails(
    @Embedded
    val workout: Workout,

    @Relation(
        entity = WorkoutSet::class,
        parentColumn = "id",
        entityColumn = "workoutId"
    )
    val sets: List<WorkoutSetWithExercise>
)
