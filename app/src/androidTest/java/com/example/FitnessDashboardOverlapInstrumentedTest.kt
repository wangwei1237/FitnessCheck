package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.database.WorkoutDatabase
import com.example.data.entity.Workout
import com.example.data.repository.FitnessRepository
import com.example.ui.screen.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FitnessViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FitnessDashboardOverlapInstrumentedTest {

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
    fun dashboard_manyPlans_lastStartButtonCanScrollAboveFabAndClick() {
        val workoutIds = runBlocking {
            (1..8).map { index ->
                repository.insertWorkout(
                    Workout(
                        date = System.currentTimeMillis(),
                        status = "PLANNED",
                        targetMuscleGroup = "计划$index"
                    )
                )
            }
        }
        val lastWorkoutId = workoutIds.last()
        var navigatedWorkoutId: Long? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToLogger = { navigatedWorkoutId = it },
                    onNavigateToLibrary = {}
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodes(hasTestTag("workout_record_card_$lastWorkoutId"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithTag("workout_log_list")
            .performScrollToNode(hasTestTag("start_workout_button_$lastWorkoutId"))

        composeTestRule
            .onNodeWithTag("start_workout_button_$lastWorkoutId")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        assertEquals(lastWorkoutId, navigatedWorkoutId)
    }
}
