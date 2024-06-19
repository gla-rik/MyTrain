package com.example.mytrain

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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

class ExerciseDetailActivity : AppCompatActivity() {

    private lateinit var exerciseNameTextView: TextView
    private lateinit var exerciseDescriptionTextView: TextView
    private lateinit var exerciseInventoryTextView: TextView
    private lateinit var exerciseGifImageView: ImageView
    private lateinit var progressBarLoadingGif: ProgressBar
    private lateinit var setsContainer: LinearLayout
    private lateinit var addSetButton: Button
    private lateinit var removeSetButton: ImageButton
    private lateinit var addExerciseButton: Button
    private var exerciseOrder: Int = -1  // Переменная для хранения порядкового номера упражнения
    private var setCount: Int = 0  // Переменная для хранения количества добавленных подходов
    private var sets = mutableListOf<Map<String, String>>()  // Массив для хранения подходов

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_detail)

        exerciseNameTextView = findViewById(R.id.textViewExerciseName)
        exerciseDescriptionTextView = findViewById(R.id.textViewExerciseDescription)
        exerciseInventoryTextView = findViewById(R.id.textViewExerciseInventory)
        exerciseGifImageView = findViewById(R.id.imageViewExerciseGif)
        progressBarLoadingGif = findViewById(R.id.progressBarLoadingGif)
        setsContainer = findViewById(R.id.setsContainer)
        addSetButton = findViewById(R.id.buttonAddSet)
        removeSetButton = findViewById(R.id.buttonRemoveSet)
        addExerciseButton = findViewById(R.id.buttonAddExercise)

        addSetButton.setOnClickListener {
            showAddSetDialog()
        }

        removeSetButton.setOnClickListener {
            removeLastSet()
        }

        addExerciseButton.setOnClickListener {
            if (sets.size <= 1) {
                showNoSetsAlert()
            } else {
                val sp = getSharedPreferences("TY", Context.MODE_PRIVATE)
                val userId = sp.getString("USER_ID", null)
                val workoutId = intent.getStringExtra("WORKOUT_ID")
                val blockId = intent.getStringExtra("BLOCK_ID")

                val blockReference = "/users/$userId/user_date_workouts/$workoutId/blocks/$blockId"

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

        Log.d("ExerciseDetailActivity", "Получена тренировка: ID = $workoutId, Название = $workoutName, Блок: ID = $blockId, Название = $blockName")

        logBlockReference(userId, workoutId, blockId) // Логирование ссылки на блок

        loadExerciseDetails(exerciseName, groupId)

        // Получение информации из SharedPreferences
        val sp = getSharedPreferences("TY", Context.MODE_PRIVATE)
        val savedEmail = sp.getString("Email", "Email не найден")
        Log.d("ExerciseDetailActivity", "SharedPreferences: Email = $savedEmail")
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

                    val intent = Intent(this, EditWorkoutDayActivity::class.java).apply {
                        putExtra("WORKOUT_ID", workoutId)
                        putExtra("WORKOUT_NAME", workoutName)
                        putExtra("BLOCK_ID", blockId)
                        putExtra("BLOCK_NAME", blockName)
                        putExtra("WORKOUT_DATE", workoutDate)
                        putExtra("COLLECTION_TYPE", "user_date_workouts")
                    }
                    startActivity(intent)
                    finish()
                }
                .show()
        }
    }




    private fun showAddSetDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_set, null)
        val repsEditText = dialogView.findViewById<EditText>(R.id.editTextReps)
        val weightEditText = dialogView.findViewById<EditText>(R.id.editTextWeight)
        val timeEditText = dialogView.findViewById<EditText>(R.id.editTextTime)

        AlertDialog.Builder(this)
            .setTitle("Добавить подход")
            .setView(dialogView)
            .setPositiveButton("Добавить") { dialog, _ ->
                var reps = repsEditText.text.toString()
                var weight = weightEditText.text.toString()
                var time = timeEditText.text.toString()

                // Устанавливаем значение "0", если поле пустое
                if (reps.isEmpty()) reps = "0"
                if (weight.isEmpty()) weight = "0"
                if (time.isEmpty()) time = "0"

                val set = mapOf(
                    "rep" to reps,
                    "weight" to weight,
                    "time" to time
                )
                sets.add(set)
                logSets()
                displaySets()

                Toast.makeText(this, "Повторения: $reps, Вес: $weight, Время: $time", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun displaySets() {
        setsContainer.removeAllViews()
        for (i in 1 until sets.size) { // начинаем с 1, потому что 0 - это ссылка на упражнение
            val set = sets[i]
            val reps = set["rep"]
            val weight = set["weight"]
            val time = set["time"]

            val setContainer = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBackgroundColor(resources.getColor(android.R.color.darker_gray)) // Задать цвет фона контейнеру
                setPadding(16, 16, 16, 16)
                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.setMargins(0, 8, 0, 8) // Визуальное отделение каждого подхода
                this.layoutParams = layoutParams
            }

            val textContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val setTitleTextView = TextView(this).apply {
                text = "Подход - $i"
                textSize = 18f
                setTypeface(typeface, android.graphics.Typeface.BOLD) // Сделать текст жирным
            }

            val setTextView = TextView(this).apply {
                text = "Повторений: $reps\nВес: $weight\nВремя: $time секунда(ы)/минут"
                textSize = 16f
            }

            val editButton = Button(this).apply {
                text = "Редактировать"
                setOnClickListener {
                    showEditSetDialog(i)
                }
            }

            textContainer.addView(setTitleTextView)
            textContainer.addView(setTextView)
            setContainer.addView(textContainer)
            setContainer.addView(editButton)
            setsContainer.addView(setContainer)
        }
    }

    private fun showEditSetDialog(index: Int) {
        val set = sets[index]
        val reps = set["rep"]
        val weight = set["weight"]
        val time = set["time"]

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_set, null)
        val repsEditText = dialogView.findViewById<EditText>(R.id.editTextReps)
        val weightEditText = dialogView.findViewById<EditText>(R.id.editTextWeight)
        val timeEditText = dialogView.findViewById<EditText>(R.id.editTextTime)

        repsEditText.setText(reps)
        weightEditText.setText(weight)
        timeEditText.setText(time)

        AlertDialog.Builder(this)
            .setTitle("Редактировать подход")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { dialog, _ ->
                var newReps = repsEditText.text.toString()
                var newWeight = weightEditText.text.toString()
                var newTime = timeEditText.text.toString()

                // Устанавливаем значение "0", если поле пустое
                if (newReps.isEmpty()) newReps = "0"
                if (newWeight.isEmpty()) newWeight = "0"
                if (newTime.isEmpty()) newTime = "0"

                val newSet = mapOf(
                    "rep" to newReps,
                    "weight" to newWeight,
                    "time" to newTime
                )

                sets[index] = newSet
                logSets()
                displaySets()

                Toast.makeText(this, "Подход обновлен", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun removeLastSet() {
        if (sets.size > 1) { // Проверяем, есть ли подходы для удаления
            sets.removeAt(sets.size - 1)
            logSets()
            displaySets()
        } else {
            Toast.makeText(this, "Нет подходов для удаления", Toast.LENGTH_SHORT).show()
        }
    }

    private fun logSets() {
        Log.d("ExerciseDetailActivity", "Sets: $sets")
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
                                Log.d("ExerciseDetailActivity", "Порядковый номер упражнения: $exerciseOrder")
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
                                Log.d("ExerciseDetailActivity", "Ссылка на упражнение в базе данных: $exercisePath")

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

        if (userIdFromSP != null && workoutId != null && blockId != null) {
            val blockReference = "/users/$userIdFromSP/user_date_workouts/$workoutId/blocks/$blockId"
            Log.d("ExerciseDetailActivity", "Работа с блоком: $blockReference")
        } else {
            Log.d("ExerciseDetailActivity", "Недостаточно данных для построения ссылки на блок. USER_ID: $userIdFromSP, WORKOUT_ID: $workoutId, BLOCK_ID: $blockId")
        }
    }

    private fun saveExerciseToDatabase(blockReference: String, exercisePath: String, sets: List<Map<String, String>>) {
        val db = FirebaseFirestore.getInstance()
        val blockDocumentRef = db.document(blockReference)

        blockDocumentRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val exercises = document.get("exercises") as? MutableList<Map<String, Any>> ?: mutableListOf()
                Log.d("Firestore", "Текущие упражнения в блоке: $exercises")

                // Создаем новый элемент exercises
                val newExercise = mutableMapOf<String, Any>(
                    "exercise_reference" to exercisePath,
                    "sets_exercise" to sets
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
                        "exercise_reference" to exercisePath,
                        "sets_exercise" to sets
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

        val intent = Intent(this, EditWorkoutDayActivity::class.java).apply {
            putExtra("WORKOUT_ID", workoutId)
            putExtra("WORKOUT_NAME", workoutName)
            putExtra("BLOCK_ID", blockId)
            putExtra("BLOCK_NAME", blockName)
            putExtra("WORKOUT_DATE", workoutDate)
            putExtra("COLLECTION_TYPE", "user_date_workouts")
        }
        startActivity(intent)
        finish()
    }

}
