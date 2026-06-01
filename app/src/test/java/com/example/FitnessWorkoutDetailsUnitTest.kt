package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.WorkoutDatabase
import com.example.data.entity.Exercise
import com.example.data.entity.Workout
import com.example.data.entity.WorkoutSet
import com.example.data.repository.FitnessRepository
import com.example.ui.viewmodel.FitnessViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FitnessWorkoutDetailsUnitTest {

    private lateinit var database: WorkoutDatabase
    private lateinit var repository: FitnessRepository
    private lateinit var viewModel: FitnessViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
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
        Dispatchers.resetMain()
    }

    @Test
    fun testStartTrainingSetsInProgressAndStartTime() = runTest(testDispatcher) {
        // Prepare a planned workout
        val workoutId = repository.insertWorkout(
            Workout(
                date = System.currentTimeMillis(),
                status = "PLANNED",
                targetMuscleGroup = "腿部"
            )
        )

        // Invoke startTraining
        viewModel.startTraining(workoutId)

        // Verify database reflects IN_PROGRESS and startTime is populated
        val updatedWorkout = repository.getWorkoutById(workoutId)
        assertNotNull(updatedWorkout)
        assertEquals("IN_PROGRESS", updatedWorkout?.status)
        assert(updatedWorkout!!.startTime > 0L)
    }

    @Test
    fun testUpdateSetParametersIncrementsPropertiesCorrectly() = runTest(testDispatcher) {
        // Prepare data with a standard workout, custom exercise and single set
        val workoutId = repository.insertWorkout(Workout(date = System.currentTimeMillis()))
        val exerciseId = repository.insertExercise(
            Exercise(
                name = "杠铃背部深蹲",
                muscleGroup = "腿部"
            )
        )
        val setId = repository.insertWorkoutSet(
            WorkoutSet(
                workoutId = workoutId,
                exerciseId = exerciseId,
                setNumber = 1,
                weight = 50.0,
                reps = 6,
                restInterval = 60
            )
        )

        val originalSet = WorkoutSet(
            id = setId,
            workoutId = workoutId,
            exerciseId = exerciseId,
            setNumber = 1,
            weight = 50.0,
            reps = 6,
            restInterval = 60
        )

        // Update set parameters with restInterval modification (+10 seconds -> 70)
        viewModel.updateSetParameters(
            set = originalSet,
            weight = 55.0,
            reps = 8,
            isCompleted = true,
            restInterval = 70
        )

        // Assert database matches the update
        val details = repository.getWorkoutWithDetailsById(workoutId).first()
        assertNotNull(details)
        val targetSet = details?.sets?.find { it.workoutSet.id == setId }?.workoutSet
        assertNotNull(targetSet)
        assertEquals(55.0, targetSet?.weight ?: 0.0, 0.0)
        assertEquals(8, targetSet?.reps ?: 0)
        assertEquals(true, targetSet?.isCompleted)
        assertEquals(70, targetSet?.restInterval ?: 0)
    }
}
