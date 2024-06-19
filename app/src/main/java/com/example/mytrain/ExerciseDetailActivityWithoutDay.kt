package com.example.mytrain

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class ExerciseDetailActivityWithoutDay : AppCompatActivity() {

    private lateinit var exerciseNameTextView: TextView
    private lateinit var exerciseDescriptionTextView: TextView
    private lateinit var exerciseInventoryTextView: TextView
    private lateinit var exerciseGifImageView: ImageView
    private lateinit var progressBarLoadingGif: ProgressBar
    private lateinit var setsContainer: LinearLayout
    private lateinit var addExerciseButton: Button
    private var exerciseOrder: Int = -1  // Переменная для хранения порядкового номера упражнения
    private var sets = mutableListOf<Map<String, String>>()  // Массив для хранения подходов

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_detail_without_day) // изменен путь к макету

        exerciseNameTextView = findViewById(R.id.textViewExerciseName)
        exerciseDescriptionTextView = findViewById(R.id.textViewExerciseDescription)
        exerciseInventoryTextView = findViewById(R.id.textViewExerciseInventory)
        exerciseGifImageView = findViewById(R.id.imageViewExerciseGif)
        progressBarLoadingGif = findViewById(R.id.progressBarLoadingGif)
        setsContainer = findViewById(R.id.setsContainer)
        addExerciseButton = findViewById(R.id.buttonAddExercise)

        addExerciseButton.setOnClickListener {
            if (sets.isEmpty()) {
                showNoSetsAlert()
            } else {
                val sp = getSharedPreferences("TY", Context.MODE_PRIVATE)
                val userId = sp.getString("USER_ID", null)
                val workoutId = intent.getStringExtra("WORKOUT_ID")
                val blockId = intent.getStringExtra("BLOCK_ID")
                val collectionType = intent.getStringExtra("COLLECTION_TYPE")

                val blockReference = if (collectionType == "user_date_workouts") {
                    "/users/$userId/user_date_workouts/$workoutId/blocks/$blockId"
                } else {
                    "/users/$userId/users_workouts/$workoutId/blocks/$blockId"
                }

                Log.d("saveExerciseToDatabase", "blockReference: $blockReference")

                val exercisePath = sets[0]["exercise_reference"] ?: ""
                val setsToSave = sets.drop(1) // Исключаем первый элемент, который является ссылкой на упражнение

                saveExerciseToDatabase(blockReference, exercisePath, setsToSave)
            }
        }

        val exerciseName = intent.getStringExtra("EXERCISE_NAME")
        val groupId = intent.getStringExtra("GROUP_ID")
        val workoutId = intent.getStringExtra("WORKOUT_ID")
        val workoutName = intent.getStringExtra("WORKOUT_NAME")
        val blockId = intent.getStringExtra("BLOCK_ID")
        val blockName = intent.getStringExtra("BLOCK_NAME")
        val userId = intent.getStringExtra("USER_ID") // Предполагаем, что USER_ID также передается через Intent

        Log.d("ExerciseDetailActivityWithoutDay", "Получена тренировка: ID = $workoutId, Название = $workoutName, Блок: ID = $blockId, Название = $blockName")

        logBlockReference(userId, workoutId, blockId) // Логирование ссылки на блок

        loadExerciseDetails(exerciseName, groupId)

        // Получение информации из SharedPreferences
        val sp = getSharedPreferences("TY", Context.MODE_PRIVATE)
        val savedEmail = sp.getString("Email", "Email не найден")
        Log.d("ExerciseDetailActivityWithoutDay", "SharedPreferences: Email = $savedEmail")
    }

    override fun onBackPressed() {
        if (!isFinishing) {
            AlertDialog.Builder(this)
                .setTitle("Несохраненная тренировка")
                .setMessage("Вы уверены, что хотите выйти? Тренировка не будет сохранена.")
                .setPositiveButton("Остаться") { dialog, _ ->
                    dialog.dismiss()
                }
                .setNegativeButton("Выйти") { dialog, _ ->
                    val workoutId = intent.getStringExtra("WORKOUT_ID")
                    val workoutName = intent.getStringExtra("WORKOUT_NAME")
                    val blockId = intent.getStringExtra("BLOCK_ID")
                    val blockName = intent.getStringExtra("BLOCK_NAME")
                    val workoutDate = intent.getStringExtra("WORKOUT_DATE")
                    val sp = getSharedPreferences("TY", Context.MODE_PRIVATE)
                    val userId = sp.getString("USER_ID", null)
                    val collectionType = intent.getStringExtra("COLLECTION_TYPE")

                    val blockReference = if (collectionType == "user_date_workouts") {
                        "/users/$userId/user_date_workouts/$workoutId/blocks/$blockId"
                    } else {
                        "/users/$userId/users_workouts/$workoutId/blocks/$blockId"
                    }

                    val isDateWorkout = blockReference.contains("user_date_workouts")
                    Log.d("onBackPressed", "isDateWorkout: $isDateWorkout")

                    val intent = if (isDateWorkout) {
                        Intent(this, EditWorkoutDayActivity::class.java).apply {
                            putExtra("WORKOUT_ID", workoutId)
                            putExtra("WORKOUT_NAME", workoutName)
                            putExtra("BLOCK_ID", blockId)
                            putExtra("BLOCK_NAME", blockName)
                            putExtra("WORKOUT_DATE", workoutDate)
                            putExtra("COLLECTION_TYPE", "user_date_workouts")
                        }
                    } else {
                        Intent(this, EditWorkoutActivity::class.java).apply {
                            putExtra("WORKOUT_ID", workoutId)
                            putExtra("WORKOUT_NAME", workoutName)
                            putExtra("BLOCK_ID", blockId)
                            putExtra("BLOCK_NAME", blockName)
                            putExtra("COLLECTION_TYPE", "users_workouts")
                        }
                    }
                    startActivity(intent)
                    finish()
                }
                .show()
        }
    }

    private fun showNoSetsAlert() {
        val exerciseName = intent.getStringExtra("EXERCISE_NAME")
        val groupId = intent.getStringExtra("GROUP_ID")
        val workoutId = intent.getStringExtra("WORKOUT_ID")
        val workoutName = intent.getStringExtra("WORKOUT_NAME")
        val blockId = intent.getStringExtra("BLOCK_ID")
        val blockName = intent.getStringExtra("BLOCK_NAME")

        AlertDialog.Builder(this)
            .setTitle("Нет подходов")
            .setMessage("Нет ни одного подхода. Хотите добавить подходы?")
            .setPositiveButton("Да") { dialog, _ ->
                dialog.dismiss()
            }
            .setNegativeButton("Нет") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, EditWorkoutActivity::class.java)
                intent.putExtra("WORKOUT_ID", workoutId)
                intent.putExtra("WORKOUT_NAME", workoutName)
                intent.putExtra("BLOCK_ID", blockId)
                intent.putExtra("BLOCK_NAME", blockName)
                startActivity(intent)
                finish()
            }
            .show()
    }

    private fun loadExerciseDetails(exerciseName: String?, groupId: String?) {
        if (exerciseName == null || groupId == null) {
            return
        }

        val db = FirebaseFirestore.getInstance()

        val documentReference = db.collection("exercises_groups").document(groupId)
        documentReference.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val exercises = document["exercises"] as? List<Map<String, Any>>
                    if (exercises != null) {
                        var index = 0
                        for (exercise in exercises) {
                            if (exercise["name"] == exerciseName) {
                                exerciseOrder = index  // Сохранение порядкового номера упражнения
                                Log.d("ExerciseDetailActivityWithoutDay", "Порядковый номер упражнения: $exerciseOrder")
                                exerciseNameTextView.text = exercise["name"] as String
                                exerciseDescriptionTextView.text = "Описание: " + (exercise["description"] as String)
                                exerciseInventoryTextView.text = "Инвентарь: " + (exercise["inventory"] as String)
                                val mediaPath = exercise["media"] as String

                                // Получить URL из Firebase Storage и загрузить GIF
                                val storageReference = FirebaseStorage.getInstance().getReference(mediaPath)
                                storageReference.downloadUrl.addOnSuccessListener { uri ->
                                    if (!isDestroyed && !isFinishing) {
                                        Glide.with(this)
                                            .asGif()
                                            .load(uri)
                                            .into(exerciseGifImageView)
                                        // Скрыть ProgressBar и показать ImageView
                                        progressBarLoadingGif.visibility = ProgressBar.GONE
                                        exerciseGifImageView.visibility = ImageView.VISIBLE
                                    }
                                }.addOnFailureListener { exception ->
                                    // Если произошла ошибка, загрузить гифку по адресу temp_folder/cat.gif
                                    val fallbackStorageReference = FirebaseStorage.getInstance().getReference("temp_folder/cat.gif")
                                    fallbackStorageReference.downloadUrl.addOnSuccessListener { fallbackUri ->
                                        if (!isDestroyed && !isFinishing) {
                                            Glide.with(this)
                                                .asGif()
                                                .load(fallbackUri)
                                                .into(exerciseGifImageView)
                                            // Скрыть ProgressBar и показать ImageView
                                            progressBarLoadingGif.visibility = ProgressBar.GONE
                                            exerciseGifImageView.visibility = ImageView.VISIBLE
                                        }
                                    }.addOnFailureListener { fallbackException ->
                                        // Обработка ошибки загрузки резервного URL
                                        if (!isDestroyed && !isFinishing) {
                                            progressBarLoadingGif.visibility = ProgressBar.GONE
                                            Toast.makeText(this, "Ошибка загрузки GIF: ${fallbackException.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }

                                // Вывести ссылку на упражнение в базе данных с учетом exerciseOrder
                                val exercisePath = "${documentReference.path}/exercises/$exerciseOrder"
                                Log.d("ExerciseDetailActivityWithoutDay", "Ссылка на упражнение в базе данных: $exercisePath")

                                // Сохранение ссылки на упражнение в массиве sets
                                sets.add(mapOf("exercise_reference" to exercisePath))

                                break
                            }
                            index++
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Обработка ошибки загрузки данных
                if (!isDestroyed && !isFinishing) {
                    progressBarLoadingGif.visibility = ProgressBar.GONE
                    Toast.makeText(this, "Ошибка загрузки данных: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun logBlockReference(userId: String?, workoutId: String?, blockId: String?) {
        val sp = getSharedPreferences("TY", Context.MODE_PRIVATE)
        val userIdFromSP = sp.getString("USER_ID", null) ?: userId
        val collectionType = intent.getStringExtra("COLLECTION_TYPE")

        if (userIdFromSP != null && workoutId != null && blockId != null) {
            val blockReference = if (collectionType == "user_date_workouts") {
                "/users/$userIdFromSP/user_date_workouts/$workoutId/blocks/$blockId"
            } else {
                "/users/$userIdFromSP/users_workouts/$workoutId/blocks/$blockId"
            }
            Log.d("ExerciseDetailActivityWithoutDay", "Работа с блоком: $blockReference")
        } else {
            Log.d("ExerciseDetailActivityWithoutDay", "Недостаточно данных для построения ссылки на блок. USER_ID: $userIdFromSP, WORKOUT_ID: $workoutId, BLOCK_ID: $blockId")
        }
    }

    private fun saveExerciseToDatabase(blockReference: String, exercisePath: String, sets: List<Map<String, String>>) {
        val db = FirebaseFirestore.getInstance()
        val blockDocumentRef = db.document(blockReference)

        blockDocumentRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val exercises = document.get("exercises") as? MutableList<Map<String, Any>> ?: mutableListOf()
                Log.d("Firestore", "Текущие упражнения в блоке: $exercises")

                // Создаем новый элемент exercises без sets_exercise
                val newExercise = mutableMapOf<String, Any>(
                    "exercise_reference" to exercisePath
                )

                exercises.add(newExercise)
                Log.d("Firestore", "Добавляемое упражнение: $newExercise")

                // Обновляем документ
                blockDocumentRef.update("exercises", exercises)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Упражнение успешно добавлено в базу данных")
                        navigateToEditWorkout(blockReference)
                    }
                    .addOnFailureListener { e ->
                        Log.w("Firestore", "Ошибка добавления упражнения в базу данных", e)
                    }
            } else {
                // Документ не существует, создаем новый
                val newExercise = listOf(
                    mapOf(
                        "exercise_reference" to exercisePath
                    )
                )
                val newBlockData = mapOf(
                    "exercises" to newExercise
                )

                Log.d("Firestore", "Создаем новый блок данных: $newBlockData")

                blockDocumentRef.set(newBlockData)
                    .addOnSuccessListener {
                        Log.d("Firestore", "Документ блока успешно создан и упражнение добавлено")
                        navigateToEditWorkout(blockReference)
                    }
                    .addOnFailureListener { e ->
                        Log.w("Firestore", "Ошибка создания документа блока и добавления упражнения", e)
                    }
            }
        }.addOnFailureListener { e ->
            Log.w("Firestore", "Ошибка получения документа блока", e)
        }
    }

    private fun navigateToEditWorkout(blockReference: String) {
        val workoutId = intent.getStringExtra("WORKOUT_ID")
        val workoutName = intent.getStringExtra("WORKOUT_NAME")
        val blockId = intent.getStringExtra("BLOCK_ID")
        val blockName = intent.getStringExtra("BLOCK_NAME")
        val workoutDate = intent.getStringExtra("WORKOUT_DATE")

        val isDateWorkout = blockReference.contains("user_date_workouts")
        Log.d("navigateToEditWorkout", "isDateWorkout: $isDateWorkout")

        val intent = if (isDateWorkout) {
            Intent(this, EditWorkoutDayActivity::class.java).apply {
                putExtra("WORKOUT_ID", workoutId)
                putExtra("WORKOUT_NAME", workoutName)
                putExtra("BLOCK_ID", blockId)
                putExtra("BLOCK_NAME", blockName)
                putExtra("WORKOUT_DATE", workoutDate)
                putExtra("COLLECTION_TYPE", "user_date_workouts")
            }
        } else {
            Intent(this, EditWorkoutActivity::class.java).apply {
                putExtra("WORKOUT_ID", workoutId)
                putExtra("WORKOUT_NAME", workoutName)
                putExtra("BLOCK_ID", blockId)
                putExtra("BLOCK_NAME", blockName)
                putExtra("COLLECTION_TYPE", "users_workouts")
            }
        }
        startActivity(intent)
        finish()
    }
}
