package com.example.mytrain

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Profile : Fragment() {
    private lateinit var weightButton: Button
    private lateinit var heightTextView: TextView
    private lateinit var bmiTextView: TextView
    private lateinit var exercisesTextView: TextView  // Новый TextView для упражнений
    private lateinit var email: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_profile, container, false)

        weightButton = view.findViewById(R.id.weightButton)
        heightTextView = view.findViewById(R.id.heightTextView)
        bmiTextView = view.findViewById(R.id.bmiTextView)
        exercisesTextView = view.findViewById(R.id.exercisesTextView)  // Инициализация TextView для упражнений

        val sp = requireActivity().getSharedPreferences("TY", Context.MODE_PRIVATE)
        email = sp.getString("Email", null).orEmpty()

        if (email.isNotEmpty()) {
            loadUserData(email)
        }

        val logoutButton: Button = view.findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            sp.edit().remove("Email").apply()
            startActivity(Intent(activity, MainActivity::class.java))
            activity?.finish()
        }

        weightButton.setOnClickListener {
            showWeightInputDialog()
        }

        // Загрузка выполненных упражнений
        loadCompletedExercises()

        return view
    }

    private fun loadUserData(email: String) {
        val db = Firebase.firestore
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.first()
                    val weightArray = document.get("weight") as? List<String>
                    val height = document.getString("height")?.toDoubleOrNull()
                    if (!weightArray.isNullOrEmpty() && height != null) {
                        val weight = weightArray.last().toDoubleOrNull()
                        if (weight != null) {
                            val bmi = calculateBMI(weight, height)
                            weightButton.text = "Вес: $weight кг"
                            heightTextView.text = "Рост: $height см"
                            bmiTextView.text = "ИМТ: $bmi"
                        }
                    }
                }
            }
    }

    private fun calculateBMI(weight: Double, height: Double): Double {
        val heightInMeters = height / 100
        return (weight / (heightInMeters * heightInMeters)).let { Math.round(it * 100) / 100.0 }
    }

    private fun showWeightInputDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Введите новый вес")

        val input = EditText(requireContext())
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val newWeight = input.text.toString().toDoubleOrNull()
            if (newWeight != null) {
                updateWeight(newWeight)
            } else {
                Toast.makeText(requireContext(), "Неверный вес", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun updateWeight(newWeight: Double) {
        val db = Firebase.firestore
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.first()
                    val weightArray = document.get("weight") as? MutableList<String> ?: mutableListOf()
                    weightArray.add(newWeight.toString())
                    document.reference.update("weight", weightArray)
                        .addOnSuccessListener {
                            val height = document.getString("height")?.toDoubleOrNull()
                            if (height != null) {
                                val bmi = calculateBMI(newWeight, height)
                                weightButton.text = "Вес: $newWeight кг"
                                bmiTextView.text = "ИМТ: $bmi"
                            }
                        }
                }
            }
    }

    private fun loadCompletedExercises() {
        val db = Firebase.firestore
        val sp = requireActivity().getSharedPreferences("TY", Context.MODE_PRIVATE)
        val userId = sp.getString("USER_ID", null)

        if (userId != null) {
            db.collection("users").document(userId).collection("user_date_workouts")
                .whereEqualTo("done", "выполнена")
                .get()
                .addOnSuccessListener { result ->
                    val exercisesInfo = mutableMapOf<String, MutableList<Pair<String, String>>>()
                    val tasks = mutableListOf<Task<*>>()

                    for (document in result) {
                        val workoutDate = document.getString("date") ?: "Unknown Date"
                        val blocksCollection = document.reference.collection("blocks")
                        tasks.add(blocksCollection.get().continueWithTask { blocksTask ->
                            val blockTasks = mutableListOf<Task<*>>()
                            val blocks = blocksTask.result
                            if (blocks != null) {
                                for (blockDoc in blocks.documents) {
                                    val exercises = blockDoc.get("exercises") as? List<Map<String, Any>> ?: emptyList()
                                    for (exercise in exercises) {
                                        val exerciseRef = exercise["exercise_reference"] as? String ?: continue
                                        val sets = exercise["sets_exercise"] as? List<Map<String, String>> ?: emptyList()
                                        val exerciseNameTask = getExerciseName(exerciseRef)
                                        blockTasks.add(exerciseNameTask.continueWith { nameTask ->
                                            val exerciseName = nameTask.result
                                            val exerciseInfo = sets.joinToString { set ->
                                                "Повторений: ${set["rep"]}, Вес: ${set["weight"]}, Время: ${set["time"]}"
                                            }
                                            if (exerciseName != null) {
                                                exercisesInfo.getOrPut(exerciseName) { mutableListOf() }
                                                    .add(Pair(workoutDate, exerciseInfo))
                                            }
                                        })
                                    }
                                }
                            }
                            Tasks.whenAll(blockTasks)
                        })
                    }

                    Tasks.whenAll(tasks)
                        .addOnSuccessListener {
                            displayExercises(exercisesInfo)
                        }
                        .addOnFailureListener { e ->
                            Log.e("Profile", "Ошибка загрузки упражнений: ${e.message}", e)
                            Toast.makeText(requireContext(), "Ошибка загрузки упражнений: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("Profile", "Ошибка загрузки упражнений: ${e.message}", e)
                    Toast.makeText(requireContext(), "Ошибка загрузки упражнений: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("Profile", "USER_ID is null")
        }
    }

    private fun getExerciseName(exerciseRef: String): Task<String?> {
        val parts = exerciseRef.split("/")
        if (parts.size == 4) {
            val collection = parts[0]
            val groupId = parts[1]
            val subcollection = parts[2]
            val exerciseIndex = parts[3].toIntOrNull() ?: -1

            if (exerciseIndex != -1) {
                val exerciseDocTask = Firebase.firestore.collection(collection).document(groupId).get()
                return exerciseDocTask.continueWith { task ->
                    val exerciseDoc = task.result
                    if (exerciseDoc != null && exerciseDoc.exists()) {
                        val exercisesArray = exerciseDoc.get(subcollection) as? List<Map<String, Any>>
                        if (exercisesArray != null && exerciseIndex < exercisesArray.size) {
                            return@continueWith exercisesArray[exerciseIndex]["name"] as? String
                        }
                    }
                    return@continueWith null
                }
            }
        }
        Log.e("Profile", "Invalid exercise reference: $exerciseRef")
        return Tasks.forResult(null)
    }

    private fun displayExercises(exercisesInfo: Map<String, List<Pair<String, String>>>) {
        Log.d("Profile", "Displaying exercises: $exercisesInfo")
        val builder = StringBuilder()
        for ((exerciseName, details) in exercisesInfo) {
            builder.append("<h2>").append(exerciseName).append("</h2>") // Заголовок для названия упражнения
            for ((date, info) in details) {
                builder.append("<h3>").append(date).append("</h3>") // Заголовок для даты
                builder.append("<p>").append(info).append("</p>") // Обычный текст для информации
                builder.append("<br/>") // Отступ между информацией о датах
            }
            builder.append("<br/><br/>") // Отступ между упражнениями
        }
        exercisesTextView.text = Html.fromHtml(builder.toString(), Html.FROM_HTML_MODE_LEGACY)
        Log.d("Profile", "Exercises displayed")
    }
}
