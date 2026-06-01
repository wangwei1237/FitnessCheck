package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 训练细节组数表实体
 * 记录每一个动作的具体做组数据（组号、重量、次数）
 *
 * @property id 组数记录ID，自增
 * @property workoutId 关联的训练日记ID (workouts)
 * @property exerciseId 关联的训练动作ID (exercises)
 * @property setNumber 当前组号（如：第一组为 1，第二组为 2）
 * @property weight 训练重量（kg）
 * @property reps 重复次数
 */
@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Exercise::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT // 避免误删已被记录的动作
        )
    ],
    indices = [
        Index("workoutId"),
        Index("exerciseId")
    ]
)
data class WorkoutSet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val isCompleted: Boolean = false,
    val restInterval: Int = 60
)
