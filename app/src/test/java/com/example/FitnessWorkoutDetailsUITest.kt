package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.WorkoutDatabase
import com.example.data.entity.Workout
import com.example.data.repository.FitnessRepository
import com.example.ui.screen.WorkoutLoggerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FitnessViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class FitnessWorkoutDetailsUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: WorkoutDatabase
    private lateinit var repository: FitnessRepository
    private lateinit var viewModel: FitnessViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        repository = FitnessRepository(database.exerciseDao(), database.workoutDao())
        viewModel = FitnessViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testWorkoutLoggerScreen_InProgress_UI() {
        var workoutId = 0L
        runBlocking {
            workoutId = repository.insertWorkout(
                Workout(
                    date = System.currentTimeMillis(),
                    status = "IN_PROGRESS",
                    targetMuscleGroup = "背部",
                    startTime = 0L // Keep 0L so infinite timer loop is skipped inside test
                )
            )
        }

        // Initialize viewmodel flow
        viewModel.loadWorkout(workoutId)

        // Render layout
        composeTestRule.setContent {
            MyApplicationTheme {
                WorkoutLoggerScreen(
                    viewModel = viewModel,
                    workoutId = workoutId,
                    onNavigateToSelectExercise = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // 1. Assert title labels display "训练中"
        composeTestRule.onNodeWithText("训练中").assertExists()

        // 2. Assert dismiss navigation icon (Close button/Arrow button) on the left of top bar is hidden during IN_PROGRESS
        composeTestRule.onNodeWithContentDescription("取消并放弃").assertDoesNotExist()

        // 3. Assert "挑选并添加训练动作" Card is hidden during IN_PROGRESS active session
        composeTestRule.onNodeWithText("挑选并添加训练动作").assertDoesNotExist()
    }

    @Test
    fun testWorkoutLoggerScreen_Planned_UI() {
        var workoutId = 0L
        runBlocking {
            workoutId = repository.insertWorkout(
                Workout(
                    date = System.currentTimeMillis(),
                    status = "PLANNED",
                    targetMuscleGroup = "胸部"
                )
            )
        }

        // Initialize viewmodel flow
        viewModel.loadWorkout(workoutId)

        // Render layout
        composeTestRule.setContent {
            MyApplicationTheme {
                WorkoutLoggerScreen(
                    viewModel = viewModel,
                    workoutId = workoutId,
                    onNavigateToSelectExercise = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // 1. Assert screen title displays "胸部 计划中"
        composeTestRule.onNodeWithText("胸部 计划中").assertExists()

        // 2. Assert close button description "取消并放弃" is visible
        composeTestRule.onNodeWithContentDescription("取消并放弃").assertExists()

        // 3. Assert "挑选并添加训练动作" action Card is visible under planned status
        composeTestRule.onNodeWithText("挑选并添加训练动作").assertExists()
    }
}
