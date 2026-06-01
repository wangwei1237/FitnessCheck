package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 训练日记主表实体
 *
 * @property id 训练记录主键ID，自增
 * @property date 训练发生的日期时间戳（毫秒数），便于在日历中按天查询
 * @property duration 训练持续时长（秒），默认为 0
 * @property note 训练备注或心得体会
 */
@Entity(tableName = "workouts")
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val duration: Long = 0,
    val note: String = "",
    val status: String = "PLANNED",
    val targetMuscleGroup: String = "全部",
    val startTime: Long = 0L
)
