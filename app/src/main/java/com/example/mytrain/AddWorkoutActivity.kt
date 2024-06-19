package com.example.mytrain

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddWorkoutActivity : AppCompatActivity() {

    private lateinit var workoutNameEditText: EditText
    private lateinit var warmupLayout: LinearLayout
    private lateinit var mainWorkoutLayout: LinearLayout
    private lateinit var stretchingLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_workout)

        workoutNameEditText = findViewById(R.id.editTextWorkoutName)
        warmupLayout = findViewById(R.id.warmupLayout)
        mainWorkoutLayout = findViewById(R.id.mainWorkoutLayout)
        stretchingLayout = findViewById(R.id.stretchingLayout)

        findViewById<Button>(R.id.buttonAddWarmupExercise).setOnClickListener {
            val intent = Intent(this, SelectWarmupExerciseActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.buttonAddMainWorkoutExercise).setOnClickListener {
            addExercise(mainWorkoutLayout)
        }

        findViewById<Button>(R.id.buttonAddStretchingExercise).setOnClickListener {
            addExercise(stretchingLayout)
        }

        findViewById<Button>(R.id.buttonSaveWorkout).setOnClickListener {
            saveWorkout()
        }
    }

    private fun addExercise(layout: LinearLayout) {
        val exerciseEditText = EditText(this)
        exerciseEditText.hint = "Название упражнения"
        exerciseEditText.textSize = 16f
        exerciseEditText.setPadding(8, 8, 8, 8)
        exerciseEditText.setBackgroundResource(R.drawable.design1)
        layout.addView(exerciseEditText)
    }

    private fun saveWorkout() {
        val workoutName = workoutNameEditText.text.toString()
        if (workoutName.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, введите название тренировки", Toast.LENGTH_LONG).show()
            return
        }

        val warmupExercises = getExercisesFromLayout(warmupLayout)
        val mainWorkoutExercises = getExercisesFromLayout(mainWorkoutLayout)
        val stretchingExercises = getExercisesFromLayout(stretchingLayout)

        // Локально сохраняем тренировку, возможно, просто выводим данные в лог или Toast
        val workout = """
            Название тренировки: $workoutName
            Разминка: $warmupExercises
            Основная тренировка: $mainWorkoutExercises
            Растяжка: $stretchingExercises
        """.trimIndent()

        Toast.makeText(this, "Тренировка сохранена локально:\n$workout", Toast.LENGTH_LONG).show()
    }

    private fun getExercisesFromLayout(layout: LinearLayout): List<String> {
        val exercises = mutableListOf<String>()
        for (i in 0 until layout.childCount) {
            val exerciseEditText = layout.getChildAt(i) as EditText
            val exerciseName = exerciseEditText.text.toString()
            if (exerciseName.isNotEmpty()) {
                exercises.add(exerciseName)
            }
        }
        return exercises
    }
}
