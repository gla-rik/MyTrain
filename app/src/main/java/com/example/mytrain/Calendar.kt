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
import java.text.SimpleDateFormat
import java.util.*

class Calendar : Fragment() {

    private val db = Firebase.firestore
    private lateinit var selectedDate: String
    private lateinit var workoutsLayout: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var noWorkoutsTextView: TextView
    private var workoutId: String? = null
    private var workoutName: String? = null
    private var workoutDate: String? = null
    private var userId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_calendar, container, false)

        // Получение аргументов, переданных в фрагмент
        arguments?.let {
            workoutId = it.getString("WORKOUT_ID")
            workoutName = it.getString("WORKOUT_NAME")
            workoutDate = it.getString("WORKOUT_DATE")
            userId = it.getString("USER_ID")
        }

        val calendarView: CalendarView = view.findViewById(R.id.calendarView)
        workoutsLayout = view.findViewById(R.id.workoutsLayout)
        progressBar = view.findViewById(R.id.progressBar)
        noWorkoutsTextView = view.findViewById(R.id.noWorkoutsTextView)
        val addWorkoutButton: Button = view.findViewById(R.id.addWorkoutButton)
        val addExistingWorkoutButton: Button = view.findViewById(R.id.addExistingWorkoutButton)

        selectedDate =
            workoutDate ?: SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date())
        loadWorkoutsForDate(selectedDate, workoutsLayout, progressBar, noWorkoutsTextView)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            selectedDate = "$dayOfMonth/${month + 1}/$year"
            Toast.makeText(requireContext(), "Вы выбрали: $selectedDate", Toast.LENGTH_SHORT).show()
            workoutsLayout.removeAllViews()
            progressBar.visibility = View.VISIBLE
            noWorkoutsTextView.visibility = View.GONE

            loadWorkoutsForDate(selectedDate, workoutsLayout, progressBar, noWorkoutsTextView)
        }

        addWorkoutButton.setOnClickListener {
            showAddWorkoutDialog()
        }

        addExistingWorkoutButton.setOnClickListener {
            showExistingWorkoutsDialog()
        }

        return view
    }

// Остальной код фрагмента остается без изменений

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
                Toast.makeText(
                    context,
                    "Название тренировки не может быть пустым",
                    Toast.LENGTH_SHORT
                ).show()
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
                        val userWorkoutsCollection =
                            db.collection("users").document(userId).collection("user_date_workouts")

                        userWorkoutsCollection.whereEqualTo("name", workoutName)
                            .whereEqualTo("date", selectedDate).get()
                            .addOnSuccessListener { documents ->
                                if (documents.isEmpty) {
                                    addWorkoutToFirestore(workoutName, userWorkoutsCollection)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Тренировка с таким названием уже существует на эту дату",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Ошибка проверки тренировки: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } else {
                        Toast.makeText(context, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "Ошибка добавления тренировки: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(context, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addWorkoutToFirestore(
        workoutName: String,
        userWorkoutsCollection: CollectionReference
    ) {
        val newWorkout = hashMapOf(
            "name" to workoutName,
            "date" to selectedDate
        )

        userWorkoutsCollection.add(newWorkout)
            .addOnSuccessListener { documentReference ->
                val workoutId = documentReference.id
                val intent = Intent(context, EditWorkoutDayActivity::class.java).apply {
                    putExtra("WORKOUT_ID", workoutId)
                    putExtra("WORKOUT_NAME", workoutName)
                    putExtra("WORKOUT_DATE", selectedDate)
                }
                startActivity(intent)
                Toast.makeText(context, "Тренировка добавлена", Toast.LENGTH_SHORT).show()
                addWorkoutButton(workoutId, workoutName)
                noWorkoutsTextView.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    "Ошибка добавления тренировки: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun loadWorkoutsForDate(
        date: String,
        workoutsLayout: LinearLayout,
        progressBar: ProgressBar,
        noWorkoutsTextView: TextView
    ) {
        val sp = requireContext().getSharedPreferences("TY", Context.MODE_PRIVATE)
        val userId = sp.getString("USER_ID", null)

        if (userId != null) {
            db.collection("users").document(userId).collection("user_date_workouts")
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener { documents ->
                    if (!isAdded) return@addOnSuccessListener // Проверка, добавлен ли фрагмент

                    workoutsLayout.removeAllViews() // Очищаем предыдущие тренировки
                    if (documents.isEmpty) {
                        noWorkoutsTextView.visibility = View.VISIBLE
                    } else {
                        noWorkoutsTextView.visibility = View.GONE
                        for (document in documents) {
                            val workoutName = document.getString("name")
                            if (workoutName != null) {
                                val button = Button(requireContext()).apply {
                                    text = workoutName
                                    setOnClickListener {
                                        // Переход к активности с подробной информацией о тренировке
                                        val intent = Intent(
                                            activity,
                                            EditWorkoutDayActivity::class.java
                                        ).apply {
                                            putExtra("WORKOUT_ID", document.id)
                                            putExtra("WORKOUT_NAME", workoutName)
                                            putExtra("WORKOUT_DATE", date)
                                        }
                                        startActivity(intent)
                                    }
                                }
                                workoutsLayout.addView(button)
                            }
                        }
                    }
                    progressBar.visibility = View.GONE
                }
                .addOnFailureListener { e ->
                    if (!isAdded) return@addOnFailureListener // Проверка, добавлен ли фрагмент

                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        "Ошибка загрузки тренировок: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun addWorkoutButton(workoutId: String, workoutName: String?) {
        if (workoutName != null && isAdded) {  // Проверяем, добавлен ли фрагмент
            val workoutButton = Button(requireContext())
            workoutButton.text = workoutName
            workoutButton.setOnClickListener {
                val intent = Intent(context, EditWorkoutDayActivity::class.java).apply {
                    putExtra("WORKOUT_ID", workoutId)
                    putExtra("WORKOUT_NAME", workoutName)
                    putExtra("WORKOUT_DATE", selectedDate)
                }
                startActivity(intent)
            }
            workoutsLayout.addView(workoutButton)
        }
    }

    private fun showExistingWorkoutsDialog() {
        val context = requireContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Выберите тренировку")

        val workoutsLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val sp = requireActivity().getSharedPreferences("TY", Context.MODE_PRIVATE)
        val userId = sp.getString("USER_ID", null)

        if (userId != null) {
            db.collection("users").document(userId).collection("users_workouts")
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        for (document in documents) {
                            val workoutName = document.getString("name")
                            val workoutId = document.id
                            if (workoutName != null) {
                                val button = Button(context).apply {
                                    text = workoutName
                                    setOnClickListener {
                                        copyWorkoutToUserDateWorkouts(workoutId, workoutName)
                                    }
                                }
                                workoutsLayout.addView(button)
                            }
                        }
                        builder.setView(workoutsLayout)
                        builder.setNegativeButton("Отмена") { dialog, _ ->
                            dialog.dismiss()
                        }
                        builder.show()
                    } else {
                        Toast.makeText(context, "Нет существующих тренировок", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Ошибка загрузки существующих тренировок: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(context, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyWorkoutToUserDateWorkouts(workoutId: String, workoutName: String) {
        val sp = requireActivity().getSharedPreferences("TY", Context.MODE_PRIVATE)
        val userId = sp.getString("USER_ID", null)

        if (userId != null) {
            val userWorkoutsCollection = db.collection("users").document(userId).collection("user_date_workouts")

            db.collection("users").document(userId).collection("users_workouts").document(workoutId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val workoutData = document.data ?: return@addOnSuccessListener

                        val newWorkout = hashMapOf(
                            "name" to workoutName,
                            "date" to selectedDate
                        )

                        userWorkoutsCollection.add(newWorkout)
                            .addOnSuccessListener { newDocumentReference ->
                                copyBlocks(userId, workoutId, newDocumentReference.id)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Ошибка копирования тренировки: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(requireContext(), "Тренировка не найдена", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Ошибка загрузки тренировки: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(requireContext(), "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyBlocks(userId: String, sourceWorkoutId: String, targetWorkoutId: String) {
        val sourceBlocksCollection = db.collection("users").document(userId)
            .collection("users_workouts").document(sourceWorkoutId).collection("blocks")
        val targetBlocksCollection = db.collection("users").document(userId)
            .collection("user_date_workouts").document(targetWorkoutId).collection("blocks")

        sourceBlocksCollection.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val blockData = document.data
                    targetBlocksCollection.add(blockData)
                        .addOnSuccessListener { newBlockReference ->
                            copyExercises(userId, sourceWorkoutId, document.id, targetWorkoutId, newBlockReference.id)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Ошибка копирования блока: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка загрузки блоков: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun copyExercises(userId: String, sourceWorkoutId: String, sourceBlockId: String, targetWorkoutId: String, targetBlockId: String) {
        val sourceExercisesCollection = db.collection("users").document(userId)
            .collection("users_workouts").document(sourceWorkoutId)
            .collection("blocks").document(sourceBlockId).collection("exercises")
        val targetExercisesCollection = db.collection("users").document(userId)
            .collection("user_date_workouts").document(targetWorkoutId)
            .collection("blocks").document(targetBlockId).collection("exercises")

        sourceExercisesCollection.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val exerciseData = document.data
                    targetExercisesCollection.add(exerciseData)
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Ошибка копирования упражнения: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                Toast.makeText(requireContext(), "Тренировка скопирована", Toast.LENGTH_SHORT).show()
                loadWorkoutsForDate(selectedDate, workoutsLayout, progressBar, noWorkoutsTextView)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Ошибка загрузки упражнений: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


}