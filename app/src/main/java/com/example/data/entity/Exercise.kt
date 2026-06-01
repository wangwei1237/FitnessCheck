package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 训练动作实体类
 *
 * @property id 动作的主键ID，自增
 * @property name 动作名称（如：杠铃卧推、深蹲、引体向上等）
 * @property muscleGroup 动作对应的肌肉部位分类（如：胸、背、腿、肩、手臂、核心等）
 * @property isCustom 是否为用户自定义动作，默认为 false (系统内置动作)
 */
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val isCustom: Boolean = false
)
