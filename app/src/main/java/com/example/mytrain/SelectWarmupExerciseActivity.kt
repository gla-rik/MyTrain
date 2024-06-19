package com.example.mytrain

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class SelectWarmupExerciseActivity : AppCompatActivity() {

    private lateinit var linearLayoutExercises: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_warmup_exercise)

        linearLayoutExercises = findViewById(R.id.linearLayoutExercises)

        loadExercises()
    }

    private fun loadExercises() {
        val db = FirebaseFirestore.getInstance()

        db.collection("exercises_groups")
            .document("group_warmup")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val exercises = document["exercises"] as? List<Map<String, Any>>
                    if (exercises != null) {
                        for (exercise in exercises) {
                            val exerciseName = exercise["name"] as? String
                            if (exerciseName != null) {
                                addExerciseButton(exerciseName)
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Документ не найден", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Ошибка загрузки данных: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun addExerciseButton(exerciseName: String) {
        val button = Button(this)
        button.text = exerciseName
        button.setOnClickListener {
            val intent = Intent(this, ExerciseDetailActivity::class.java)
            intent.putExtra("EXERCISE_NAME", exerciseName)
            startActivity(intent)
        }
        linearLayoutExercises.addView(button)
    }
}
