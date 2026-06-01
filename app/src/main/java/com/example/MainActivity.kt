package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.database.WorkoutDatabase
import com.example.data.repository.FitnessRepository
import com.example.ui.screen.DashboardScreen
import com.example.ui.screen.ExerciseLibraryScreen
import com.example.ui.screen.WorkoutLoggerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FitnessViewModel
import com.example.ui.viewmodel.FitnessViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. 初始化本地 Room 数据库和仓储实例
        val database = WorkoutDatabase.getDatabase(this)
        val repository = FitnessRepository(database.exerciseDao(), database.workoutDao())
        val viewModelFactory = FitnessViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                // 2. 初始化核心逻辑控中心 ViewModel
                val fitnessViewModel: FitnessViewModel = viewModel(factory = viewModelFactory)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FitnessAppNavigator(viewModel = fitnessViewModel)
                }
            }
        }
    }
}

/**
 * FitnessCheck 全局路由导航图
 */
@Composable
fun FitnessAppNavigator(viewModel: FitnessViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        // A. 日历打卡首页
        composable("dashboard") {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToLogger = { workoutId ->
                    navController.navigate("workout_logger/$workoutId")
                },
                onNavigateToLibrary = {
                    // -1 代表普通动作库浏览，非选配注入动作模式
                    navController.navigate("exercise_library/-1")
                }
            )
        }

        // B. 训练动作库 (包含挑选模式)
        composable(
            route = "exercise_library/{selectForWorkoutId}",
            arguments = listOf(
                navArgument("selectForWorkoutId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val selectForWorkoutId = backStackEntry.arguments?.getLong("selectForWorkoutId") ?: -1L
            val parsedSelectId = if (selectForWorkoutId == -1L) null else selectForWorkoutId

            ExerciseLibraryScreen(
                viewModel = viewModel,
                selectForWorkoutId = parsedSelectId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // C. 训练记录纸面 (Workout Logger)
        composable(
            route = "workout_logger/{workoutId}",
            arguments = listOf(
                navArgument("workoutId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val workoutId = backStackEntry.arguments?.getLong("workoutId") ?: -1L

            WorkoutLoggerScreen(
                viewModel = viewModel,
                workoutId = workoutId,
                onNavigateToSelectExercise = { activeWorkoutId ->
                    navController.navigate("exercise_library/$activeWorkoutId")
                },
                onNavigateBack = {
                    // 保存/完成/退出后回到首页，并确保退栈清爽
                    navController.popBackStack("dashboard", inclusive = false)
                }
            )
        }
    }
}
