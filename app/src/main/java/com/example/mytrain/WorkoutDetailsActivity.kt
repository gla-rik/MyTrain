package com.example.mytrain

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class WorkoutDetailsActivity : AppCompatActivity() {

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_details)

        val workoutId = intent.getStringExtra("workout_id")

        if (workoutId != null) {
            db.collection("users").document("USER_ID").collection("user_date_workouts").document(workoutId)
                .get()
                .addOnSuccessListener { document ->
                    val workoutName = document.getString("name")
                    val workoutDate = document.getString("date")
                    val workoutDetails = document.getString("details")

                    findViewById<TextView>(R.id.workoutName).text = workoutName
                    findViewById<TextView>(R.id.workoutDate).text = workoutDate
                    findViewById<TextView>(R.id.workoutDetails).text = workoutDetails
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка загрузки деталей тренировки: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
