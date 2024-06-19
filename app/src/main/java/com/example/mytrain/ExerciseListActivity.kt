package com.example.mytrain

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ExerciseListActivity : AppCompatActivity() {

    private lateinit var exercisesLayout: LinearLayout
    private lateinit var groupNameTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_list)

        exercisesLayout = findViewById(R.id.exercisesLayout)
        groupNameTextView = findViewById(R.id.textViewGroupName)
        progressBar = findViewById(R.id.progressBar)
        db = FirebaseFirestore.getInstance()

        val groupId = intent.getStringExtra("GROUP_ID") ?: return
        val groupName = intent.getStringExtra("GROUP_NAME") ?: "Группа упражнений"
        val workoutId = intent.getStringExtra("WORKOUT_ID")
        val workoutName = intent.getStringExtra("WORKOUT_NAME")
        val blockId = intent.getStringExtra("BLOCK_ID")
        val blockName = intent.getStringExtra("BLOCK_NAME")

        Log.d("ExerciseListActivity", "Получена тренировка: ID = $workoutId, Название = $workoutName, Блок: ID = $blockId, Название = $blockName")

        groupNameTextView.text = "$groupName"

        // Показать ProgressBar при начале загрузки упражнений
        progressBar.visibility = View.VISIBLE
        exercisesLayout.visibility = View.GONE

        loadExercises(groupId, workoutId, workoutName, blockId, blockName)
    }

    private fun loadExercises(groupId: String, workoutId: String?, workoutName: String?, blockId: String?, blockName: String?) {
        val collectionType = intent.getStringExtra("COLLECTION_TYPE")
        Log.d("ExerciseListActivity", "COLLECTION_TYPE: $collectionType")

        db.collection("exercises_groups").document(groupId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val exercises = document.get("exercises") as? List<Map<String, Any>>
                    exercises?.forEach { exercise ->
                        val exerciseName = exercise["name"] as? String
                        if (exerciseName != null) {
                            addButton(exerciseName, groupId, workoutId, workoutName, blockId, blockName)
                        }
                    }
                } else {
                    Toast.makeText(this, "Документ не найден", Toast.LENGTH_SHORT).show()
                }
                // Скрыть ProgressBar и показать упражнения после завершения загрузки
                progressBar.visibility = View.GONE
                exercisesLayout.visibility = View.VISIBLE
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка загрузки упражнений: ${e.message}", Toast.LENGTH_SHORT).show()
                // Скрыть ProgressBar и показать упражнения даже при ошибке
                progressBar.visibility = View.GONE
                exercisesLayout.visibility = View.VISIBLE
            }
    }


    private fun addButton(exerciseName: String, groupId: String, workoutId: String?, workoutName: String?, blockId: String?, blockName: String?) {
        val button = Button(this)
        button.text = exerciseName
        button.setOnClickListener {
            val collectionType = intent.getStringExtra("COLLECTION_TYPE")
            val intent = if (collectionType == "users_workouts") {
                Intent(this, ExerciseDetailActivityWithoutDay::class.java)
            } else {
                Intent(this, ExerciseDetailActivity::class.java)
            }
            intent.putExtra("EXERCISE_NAME", exerciseName)
            intent.putExtra("GROUP_ID", groupId)
            intent.putExtra("WORKOUT_ID", workoutId)
            intent.putExtra("WORKOUT_NAME", workoutName)
            intent.putExtra("BLOCK_ID", blockId)
            intent.putExtra("BLOCK_NAME", blockName)
            intent.putExtra("COLLECTION_TYPE", collectionType)
            startActivity(intent)
        }
        exercisesLayout.addView(button)
    }



    override fun onBackPressed() {
        val intent = Intent(this, SelectGroupActivity::class.java)
        startActivity(intent)
        finish()
    }


}
