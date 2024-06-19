package com.example.mytrain

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Workouts : Fragment() {

    private lateinit var workoutLayout: LinearLayout
    private lateinit var db: FirebaseFirestore
    private lateinit var progressBar: ProgressBar
    private lateinit var noWorkoutsImage: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.workouts_activity, container, false)

        val addButton: Button = view.findViewById(R.id.btn_add_workout)
        workoutLayout = view.findViewById(R.id.ll_workout_buttons)
        progressBar = view.findViewById(R.id.progress_bar)
        noWorkoutsImage = view.findViewById(R.id.iv_no_workouts)
        db = Firebase.firestore

        addButton.setOnClickListener {
            showAddWorkoutDialog()
        }

        loadWorkouts()

        return view
    }

    private fun showAddWorkoutDialog() {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Название тренировки")

        val input = EditText(context)
        input.hint = "Введите название тренировки"
        builder.setView(input)

        builder.setPositiveButton("Добавить") { dialog, _ ->
            val workoutName = input.text.toString().trim().lowercase()
            if (workoutName.isNotEmpty()) {
                checkAndAddWorkout(workoutName)
            } else {
                Toast.makeText(context, "Название тренировки не может быть пустым", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun checkAndAddWorkout(workoutName: String) {
        val sp = requireActivity().getSharedPreferences("TY", Context.MODE_PRIVATE)
        val savedEmail = sp.getString("Email", null)

        if (savedEmail != null) {
            db.collection("users")
                .whereEqualTo("email", savedEmail)
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val userId = result.documents[0].id
                        val userWorkoutsCollection = db.collection("users").document(userId).collection("users_workouts")

                        userWorkoutsCollection.whereEqualTo("name", workoutName).get()
                            .addOnSuccessListener { documents ->
                                if (documents.isEmpty) {
                                    addWorkoutToFirestore(workoutName, userWorkoutsCollection)
                                } else {
                                    Toast.makeText(context, "Тренировка с таким названием уже существует", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(context, "Ошибка проверки тренировки: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Ошибка добавления тренировки: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addWorkoutToFirestore(workoutName: String, userWorkoutsCollection: CollectionReference) {
        val newWorkout = hashMapOf(
            "name" to workoutName
        )

        userWorkoutsCollection.add(newWorkout)
            .addOnSuccessListener { documentReference ->
                val workoutId = documentReference.id
                val intent = Intent(context, EditWorkoutActivity::class.java).apply {
                    putExtra("WORKOUT_ID", workoutId)
                    putExtra("WORKOUT_NAME", workoutName)
                }
                startActivity(intent)
                Toast.makeText(context, "Тренировка добавлена", Toast.LENGTH_SHORT).show()
                addWorkoutButton(workoutId, workoutName)
                noWorkoutsImage.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Ошибка добавления тренировки: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadWorkouts() {
        progressBar.visibility = View.VISIBLE
        val sp = requireActivity().getSharedPreferences("TY", Context.MODE_PRIVATE)
        val savedEmail = sp.getString("Email", null)

        if (savedEmail != null) {
            db.collection("users")
                .whereEqualTo("email", savedEmail)
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        val userId = result.documents[0].id
                        val userWorkoutsCollection = db.collection("users").document(userId).collection("users_workouts")

                        userWorkoutsCollection.get()
                            .addOnCompleteListener { task ->
                                progressBar.visibility = View.GONE
                                if (task.isSuccessful) {
                                    if (task.result!!.isEmpty) {
                                        noWorkoutsImage.visibility = View.VISIBLE
                                    } else {
                                        for (document in task.result!!) {
                                            val workoutName = document.getString("name")
                                            val workoutId = document.id
                                            if (isAdded) {  // Проверяем, добавлен ли фрагмент перед вызовом метода
                                                addWorkoutButton(workoutId, workoutName)
                                            }
                                        }
                                        noWorkoutsImage.visibility = View.GONE
                                    }
                                } else {
                                    if (isAdded) {
                                        Toast.makeText(requireContext(), "Ошибка загрузки тренировок", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                    } else {
                        progressBar.visibility = View.GONE
                        noWorkoutsImage.visibility = View.VISIBLE
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    noWorkoutsImage.visibility = View.VISIBLE
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Ошибка загрузки данных: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            progressBar.visibility = View.GONE
            noWorkoutsImage.visibility = View.VISIBLE
            if (isAdded) {
                Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addWorkoutButton(workoutId: String, workoutName: String?) {
        if (workoutName != null && isAdded) {  // Проверяем, добавлен ли фрагмент
            val workoutButton = Button(requireContext())
            workoutButton.text = workoutName
            workoutButton.setOnClickListener {
                val intent = Intent(context, EditWorkoutActivity::class.java).apply {
                    putExtra("WORKOUT_ID", workoutId)
                    putExtra("WORKOUT_NAME", workoutName)
                }
                startActivity(intent)
            }
            workoutLayout.addView(workoutButton)
        }
    }
}
