package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.ExerciseDao
import com.example.data.dao.WorkoutDao
import com.example.data.entity.Exercise
import com.example.data.entity.Workout
import com.example.data.entity.WorkoutSet

/**
 * 训练管理应用 Room 数据库主类
 * 管理 Exercise, Workout, 及其子表 WorkoutSet 的本地存储与持久化操作
 */
@Database(
    entities = [Exercise::class, Workout::class, WorkoutSet::class],
    version = 2,
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        /**
         * 获取单例数据库实例。双重检验锁 (Double-Checked Locking) 保证线程安全
         */
        fun getDatabase(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "fitness_workout_database"
                )
                // 在开发初期，若结构有轻微变更，可使用破坏性迁移
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
