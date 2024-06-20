package com.example.mytrain

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class EditWorkoutDayActivity : AppCompatActivity() {

    private lateinit var workoutNameTextView: TextView
    private lateinit var editWorkoutNameButton: ImageButton
    private lateinit var addBlockButton: Button
    private lateinit var blocksLayout: LinearLayout
    private lateinit var db: FirebaseFirestore
    private var workoutId: String? = null
    private var workoutName: String? = null
    private var workoutDate: String? = null
    private var userId: String? = null
    private lateinit var deleteWorkoutButton: Button  // Новое поле для кнопки "Удалить тренировку"
    private lateinit var completeWorkoutButton: Button  // Новое поле для кнопки "Выполнил"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_day_workout)

        workoutNameTextView = findViewById(R.id.workoutNameTextView)
        editWorkoutNameButton = findViewById(R.id.editWorkoutNameButton)
        addBlockButton = findViewById(R.id.addBlockButton)
        deleteWorkoutButton = findViewById(R.id.deleteWorkoutButton)  // Инициализация кнопки "Удалить тренировку"
        completeWorkoutButton = findViewById(R.id.completeWorkoutButton)  // Инициализация кнопки "Выполнил"
        blocksLayout = findViewById(R.id.blocksLayout)
        db = Firebase.firestore

        workoutId = intent.getStringExtra("WORKOUT_ID")
        workoutName = intent.getStringExtra("WORKOUT_NAME")
        workoutDate = intent.getStringExtra("WORKOUT_DATE")
        val blockId = intent.getStringExtra("BLOCK_ID")
        val blockName = intent.getStringExtra("BLOCK_NAME")

        Log.d("EditWorkoutDayActivity", "Получена тренировка: ID = $workoutId, Название = $workoutName, Дата = $workoutDate, Блок: ID = $blockId, Название = $blockName")

        workoutNameTextView.text = workoutName

        editWorkoutNameButton.setOnClickListener {
            showEditWorkoutNameDialog()
        }

        addBlockButton.setOnClickListener {
            showAddBlockDialog()
        }

        deleteWorkoutButton.setOnClickListener {
            showDeleteWorkoutDialog()
        }

        completeWorkoutButton.setOnClickListener {
            markWorkoutAsCompleted()
        }

        loadUserId {
            loadWorkoutStatus()  // Проверка состояния выполнения тренировки
            loadBlocks()
        }
    }

    private fun loadWorkoutStatus() {
        if (userId != null && workoutId != null) {
            val workoutDocRef = db.collection("users").document(userId!!).collection("user_date_workouts").document(workoutId!!)

            workoutDocRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val doneStatus = document.getString("done")
                    if (doneStatus == "выполнена") {
                        completeWorkoutButton.visibility = Button.GONE
                    } else {
                        completeWorkoutButton.visibility = Button.VISIBLE
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("EditWorkoutDayActivity", "Ошибка загрузки состояния тренировки: ${e.message}", e)
            }
        }
    }

    private fun markWorkoutAsCompleted() {
        if (userId != null && workoutId != null) {
            val workoutDocRef = db.collection("users").document(userId!!).collection("user_date_workouts").document(workoutId!!)

            workoutDocRef.update("done", "выполнена")
                .addOnSuccessListener {
                    Log.d("EditWorkoutDayActivity", "Тренировка отмечена как выполнена")
                    completeWorkoutButton.visibility = Button.GONE
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка обновления состояния тренировки: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun loadUserId(onSuccess: () -> Unit) {
        val sp = getSharedPreferences("TY", Context.MODE_PRIVATE)
        val savedEmail = sp.getString("Email", null)

        if (savedEmail != null) {
            db.collection("users")
                .whereEqualTo("email", savedEmail)
                .get()
                .addOnSuccessListener { result ->
                    if (!result.isEmpty) {
                        userId = result.documents[0].id
                        onSuccess()
                    } else {
                        Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка получения пользователя: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Пользователь не авторизован", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditWorkoutNameDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Редактировать название тренировки")

        val input = EditText(this)
        input.setText(workoutName)
        builder.setView(input)

        builder.setPositiveButton("Сохранить") { dialog, _ ->
            val newWorkoutName = input.text.toString().trim().lowercase()
            if (newWorkoutName.isNotEmpty() && workoutId != null) {
                updateWorkoutNameInFirestore(newWorkoutName)
            } else {
                Toast.makeText(this, "Название тренировки не может быть пустым", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun updateWorkoutNameInFirestore(newWorkoutName: String) {
        if (userId != null && workoutId != null) {
            val userWorkoutsCollection = db.collection("users").document(userId!!).collection("user_date_workouts")

            userWorkoutsCollection.document(workoutId!!)
                .update("name", newWorkoutName)
                .addOnSuccessListener {
                    workoutNameTextView.text = newWorkoutName
                    Toast.makeText(this, "Название тренировки обновлено", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка обновления названия: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Пользователь не авторизован или тренировка не найдена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddBlockDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Добавить блок упражнений")

        val input = EditText(this)
        input.hint = "Введите название блока"
        builder.setView(input)

        builder.setPositiveButton("Добавить") { dialog, _ ->
            val blockName = input.text.toString().trim()
            if (blockName.isNotEmpty()) {
                checkAndAddBlock(blockName)
            } else {
                Toast.makeText(this, "Название блока не может быть пустым", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun checkAndAddBlock(blockName: String) {
        if (userId != null && workoutId != null) {
            val blocksCollection = db.collection("users").document(userId!!).collection("user_date_workouts").document(workoutId!!).collection("blocks")

            blocksCollection.whereEqualTo("name", blockName).get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        addBlockToFirestore(blockName, blocksCollection)
                    } else {
                        Toast.makeText(this, "Блок с таким названием уже существует", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка проверки блока: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Тренировка не найдена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addBlockToFirestore(blockName: String, blocksCollection: CollectionReference) {
        blocksCollection.get()
            .addOnSuccessListener { documents ->
                val count = documents.size() + 1
                val newBlock = hashMapOf(
                    "name" to blockName,
                    "count" to count.toString()  // Сохранение как строку
                )

                blocksCollection.add(newBlock)
                    .addOnSuccessListener { documentReference ->
                        val blockId = documentReference.id
                        Toast.makeText(this, "Блок добавлен", Toast.LENGTH_SHORT).show()
                        addBlockButton(blockName, blockId, count)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Ошибка добавления блока: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка получения количества блоков: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadBlocks() {
        if (userId != null && workoutId != null) {
            val blocksCollection = db.collection("users").document(userId!!).collection("user_date_workouts").document(workoutId!!).collection("blocks")

            blocksCollection.orderBy("count").get()
                .addOnSuccessListener { documents ->
                    blocksLayout.removeAllViews()
                    for (document in documents) {
                        val blockName = document.getString("name")
                        val countString = document.getString("count")
                        val count = countString?.toIntOrNull() ?: 0  // Преобразование строки в Int
                        if (blockName != null) {
                            Log.d("EditWorkoutDayActivity", "Загружен блок: $blockName с count: $count")
                            addBlockButton(blockName, document.id, count)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка загрузки блоков: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun addBlockButton(blockName: String, blockId: String, count: Int) {
        val blockLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 16)
        }

        val blockNameLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val blockTextView = TextView(this).apply {
            text = "$count. $blockName"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 18f
        }

        blockNameLayout.addView(blockTextView)

        val exercisesLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 0, 0, 0)
        }

        blockLayout.addView(blockNameLayout)
        blockLayout.addView(exercisesLayout)

        val addExerciseButton = Button(this).apply {
            text = "Добавить упражнение"
            setBackgroundColor(resources.getColor(android.R.color.holo_green_light))
            setOnClickListener {
                val intent = Intent(this@EditWorkoutDayActivity, SelectGroupActivity::class.java).apply {
                    putExtra("WORKOUT_ID", workoutId)
                    putExtra("WORKOUT_NAME", workoutName)
                    putExtra("BLOCK_ID", blockId)
                    putExtra("BLOCK_NAME", blockName)
                    putExtra("COLLECTION_TYPE", "user_date_workouts")
                }
                startActivity(intent)
            }
        }

        loadExercisesForBlock(blockId, exercisesLayout)

        blockLayout.addView(addExerciseButton)
        blocksLayout.addView(blockLayout)
    }



    private fun loadExercisesForBlock(blockId: String, exercisesLayout: LinearLayout) {
        val blockDocRef = db.collection("users").document(userId!!).collection("user_date_workouts").document(workoutId!!).collection("blocks").document(blockId)

        Log.d("EditWorkoutDayActivity", "Block reference: ${blockDocRef.path}")

        blockDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val blockData = mutableMapOf<String, Any?>()
                blockData["name"] = document.getString("name")
                blockData["exercises"] = document.get("exercises") as? MutableList<Map<String, Any>>

                Log.d("EditWorkoutDayActivity", "Block data: $blockData")

                val exercises = blockData["exercises"] as? MutableList<Map<String, Any>>
                if (exercises != null) {
                    exercisesLayout.removeAllViews()

                    for ((exerciseIndex, exercise) in exercises.withIndex()) {
                        val exerciseReference = exercise["exercise_reference"] as? String
                        val sets = exercise["sets_exercise"] as? List<Map<String, String>>
                        if (exerciseReference != null) {
                            Log.d("EditWorkoutDayActivity", "Exercise reference: $exerciseReference")

                            val exerciseRefParts = exerciseReference.split("/")
                            if (exerciseRefParts.size == 4) {
                                val collection = exerciseRefParts[0]
                                val groupId = exerciseRefParts[1]
                                val subcollection = exerciseRefParts[2]
                                val exercisePosition = exerciseRefParts[3].toIntOrNull() ?: -1

                                if (exercisePosition != -1) {
                                    val exerciseRef = db.collection(collection).document(groupId)
                                    exerciseRef.get().addOnSuccessListener { exerciseDoc ->
                                        if (exerciseDoc.exists()) {
                                            val exercisesArray = exerciseDoc.get(subcollection) as? List<Map<String, Any>>
                                            if (exercisesArray != null && exercisePosition < exercisesArray.size) {
                                                val exerciseData = exercisesArray[exercisePosition]
                                                val exerciseName = exerciseData["name"] as? String ?: "Unnamed Exercise"
                                                val exerciseDescription = exerciseData["description"] as? String ?: "No description available"
                                                val exerciseInventory = exerciseData["inventory"] as? String ?: "No inventory needed"
                                                val exerciseMediaPath = exerciseData["media"] as? String
                                                Log.d("EditWorkoutDayActivity", "Загружено упражнение: $exerciseName для блока: $blockId")

                                                val exerciseLayout = LinearLayout(this).apply {
                                                    orientation = LinearLayout.VERTICAL
                                                    setPadding(0, 8, 0, 8)
                                                }

                                                val buttonLayout = LinearLayout(this).apply {
                                                    orientation = LinearLayout.HORIZONTAL
                                                }

                                                val exerciseButton = Button(this).apply {
                                                    text = exerciseName
                                                    textSize = 16f
                                                    setBackgroundColor(resources.getColor(android.R.color.holo_blue_light))
                                                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                                                    setOnClickListener {
                                                        val intent = Intent(this@EditWorkoutDayActivity, ExerciseInfoActivity::class.java).apply {
                                                            putExtra("EXERCISE_NAME", exerciseName)
                                                            putExtra("EXERCISE_DESCRIPTION", exerciseDescription)
                                                            putExtra("EXERCISE_INVENTORY", exerciseInventory)
                                                            putExtra("EXERCISE_MEDIA_PATH", exerciseMediaPath)
                                                        }
                                                        startActivity(intent)
                                                    }
                                                }

                                                val deleteButton = ImageButton(this).apply {
                                                    setImageResource(android.R.drawable.ic_menu_delete)
                                                    setBackgroundColor(resources.getColor(android.R.color.holo_red_light))
                                                    setOnClickListener {
                                                        showDeleteExerciseDialog(blockId, exerciseIndex)
                                                    }
                                                }

                                                buttonLayout.addView(exerciseButton)
                                                buttonLayout.addView(deleteButton)

                                                exerciseLayout.addView(buttonLayout)

                                                // Новая кнопка "Добавить подход"
                                                val addSetButton = Button(this).apply {
                                                    text = "Добавить подход"
                                                    setOnClickListener {
                                                        showAddSetDialog(blockId, exerciseIndex)
                                                    }
                                                }

                                                exerciseLayout.addView(addSetButton)

                                                exercisesLayout.addView(exerciseLayout)

                                                if (sets != null) {
                                                    for ((setIndex, set) in sets.withIndex()) {
                                                        val setLayout = LinearLayout(this).apply {
                                                            orientation = LinearLayout.HORIZONTAL
                                                            setPadding(16, 0, 0, 8)
                                                        }

                                                        val setTextView = TextView(this).apply {
                                                            text = "Подход ${setIndex + 1}: Повторений: ${set["rep"]}, Вес: ${set["weight"]}, Время: ${set["time"]}"
                                                            textSize = 14f
                                                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                                                        }

                                                        val editSetButton = Button(this).apply {
                                                            text = "Редактировать"
                                                            setOnClickListener {
                                                                showEditSetDialog(exerciseIndex, setIndex, set, blockId)
                                                            }
                                                        }

                                                        val deleteSetButton = Button(this).apply {
                                                            text = "Удалить"
                                                            setOnClickListener {
                                                                deleteSetFromFirestore(blockId, exerciseIndex, setIndex)
                                                            }
                                                        }

                                                        setLayout.addView(setTextView)
                                                        setLayout.addView(editSetButton)
                                                        setLayout.addView(deleteSetButton)
                                                        exercisesLayout.addView(setLayout)
                                                    }
                                                }
                                            } else {
                                                Log.e("EditWorkoutDayActivity", "Ошибка извлечения имени упражнения: индекс упражнения неверный или данные отсутствуют")
                                            }
                                        } else {
                                            Log.e("EditWorkoutDayActivity", "Документ упражнения не существует: ${exerciseRef.path}")
                                        }
                                    }.addOnFailureListener { e ->
                                        Log.e("EditWorkoutDayActivity", "Ошибка загрузки деталей упражнения: ${exerciseRef.path}", e)
                                    }
                                } else {
                                    Log.e("EditWorkoutDayActivity", "Индекс упражнения неверный: $exerciseReference")
                                }
                            } else {
                                Log.e("EditWorkoutDayActivity", "Ссылка на упражнение имеет неверный формат: $exerciseReference")
                            }
                        } else {
                            Log.d("EditWorkoutDayActivity", "Упражнение без ссылки или подходов в блоке: $blockId")
                        }
                    }
                } else {
                    Log.d("EditWorkoutDayActivity", "Нет упражнений для блока: $blockId")
                }
            } else {
                Log.e("EditWorkoutDayActivity", "Документ блока не существует: $blockId")
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Ошибка загрузки упражнений: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("EditWorkoutDayActivity", "Ошибка загрузки упражнений для блока: $blockId", e)
        }
    }

    private fun deleteSetFromFirestore(blockId: String, exerciseIndex: Int, setIndex: Int) {
        if (userId != null && workoutId != null) {
            val blockDocRef = db.collection("users")
                .document(userId!!)
                .collection("user_date_workouts")
                .document(workoutId!!)
                .collection("blocks")
                .document(blockId)

            blockDocRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val exercises = document.get("exercises") as? MutableList<Map<String, Any>>
                    if (exercises != null && exerciseIndex < exercises.size) {
                        val exercise = exercises[exerciseIndex].toMutableMap()
                        val sets = exercise["sets_exercise"] as? MutableList<Map<String, String>>
                        if (sets != null && setIndex < sets.size) {
                            sets.removeAt(setIndex) // Удаляем подход
                            exercise["sets_exercise"] = sets
                            exercises[exerciseIndex] = exercise
                            blockDocRef.update("exercises", exercises)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Подход удален", Toast.LENGTH_SHORT).show()
                                    loadBlocks()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Ошибка удаления подхода: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка загрузки блока: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddSetDialog(blockId: String, exerciseIndex: Int) {
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
                addSetToFirestore(blockId, exerciseIndex, set)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun addSetToFirestore(blockId: String, exerciseIndex: Int, newSet: Map<String, String>) {
        if (userId != null && workoutId != null) {
            val blockDocRef = db.collection("users")
                .document(userId!!)
                .collection("user_date_workouts")
                .document(workoutId!!)
                .collection("blocks")
                .document(blockId)

            blockDocRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val exercises = document.get("exercises") as? MutableList<Map<String, Any>>
                    if (exercises != null && exerciseIndex < exercises.size) {
                        val exercise = exercises[exerciseIndex].toMutableMap()
                        val sets = exercise["sets_exercise"] as? MutableList<Map<String, String>> ?: mutableListOf()
                        sets.add(newSet) // Добавляем новый подход
                        exercise["sets_exercise"] = sets
                        exercises[exerciseIndex] = exercise
                        blockDocRef.update("exercises", exercises)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Подход добавлен", Toast.LENGTH_SHORT).show()
                                loadBlocks()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Ошибка добавления подхода: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка загрузки блока: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showDeleteExerciseDialog(blockId: String, exerciseIndex: Int) {
        AlertDialog.Builder(this)
            .setTitle("Удалить упражнение")
            .setMessage("Вы уверены, что хотите удалить это упражнение?")
            .setPositiveButton("Да") { dialog, _ ->
                deleteExerciseFromFirestore(blockId, exerciseIndex)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun showEditSetDialog(exerciseIndex: Int, setIndex: Int, set: Map<String, String>, blockId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_set, null)
        val repsEditText = dialogView.findViewById<EditText>(R.id.editTextReps)
        val weightEditText = dialogView.findViewById<EditText>(R.id.editTextWeight)
        val timeEditText = dialogView.findViewById<EditText>(R.id.editTextTime)

        repsEditText.setText(set["rep"])
        weightEditText.setText(set["weight"])
        timeEditText.setText(set["time"])

        AlertDialog.Builder(this)
            .setTitle("Редактировать подход")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { dialog, _ ->
                val newReps = repsEditText.text.toString().ifEmpty { "0" }
                val newWeight = weightEditText.text.toString().ifEmpty { "0" }
                val newTime = timeEditText.text.toString().ifEmpty { "0" }

                val updatedSet = mapOf(
                    "rep" to newReps,
                    "weight" to newWeight,
                    "time" to newTime
                )

                updateSetInFirestore(blockId, exerciseIndex, setIndex, updatedSet)
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun updateSetInFirestore(blockId: String, exerciseIndex: Int, setIndex: Int, updatedSet: Map<String, String>) {
        if (userId != null && workoutId != null) {
            val blockDocRef = db.collection("users")
                .document(userId!!)
                .collection("user_date_workouts")
                .document(workoutId!!)
                .collection("blocks")
                .document(blockId)

            blockDocRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val exercises = document.get("exercises") as? MutableList<Map<String, Any>>
                    if (exercises != null && exerciseIndex < exercises.size) {
                        val exercise = exercises[exerciseIndex].toMutableMap()
                        val sets = exercise["sets_exercise"] as? MutableList<Map<String, String>>
                        if (sets != null && setIndex < sets.size) {
                            sets[setIndex] = updatedSet // Обновляем подход
                            exercise["sets_exercise"] = sets
                            blockDocRef.update("exercises", exercises)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Подход обновлен", Toast.LENGTH_SHORT).show()
                                    loadBlocks()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Ошибка обновления подхода: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка загрузки блока: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteExerciseFromFirestore(blockId: String, exerciseIndex: Int) {
        if (userId != null && workoutId != null) {
            val blockDocRef = db.collection("users")
                .document(userId!!)
                .collection("user_date_workouts")
                .document(workoutId!!)
                .collection("blocks")
                .document(blockId)

            blockDocRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val exercises = document.get("exercises") as? MutableList<Map<String, Any>>
                    if (exercises != null && exerciseIndex < exercises.size) {
                        exercises.removeAt(exerciseIndex)
                        blockDocRef.update("exercises", exercises)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Упражнение удалено", Toast.LENGTH_SHORT).show()
                                loadBlocks()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Ошибка удаления упражнения: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Ошибка загрузки блока: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteWorkout() {
        if (userId != null && workoutId != null) {
            val userWorkoutsCollection = db.collection("users").document(userId!!).collection("user_date_workouts")

            userWorkoutsCollection.document(workoutId!!)
                .delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Тренировка удалена", Toast.LENGTH_SHORT).show()
                    finish()  // Закрыть текущую активность
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Ошибка удаления тренировки: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "Пользователь не авторизован или тренировка не найдена", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteWorkoutDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Удалить тренировку")
        builder.setMessage("Вы действительно хотите удалить тренировку?")

        builder.setPositiveButton("Да") { dialog, _ ->
            deleteWorkout()
            dialog.dismiss()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
        }

        builder.show()
    }

    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("WORKOUT_ID", workoutId)
            putExtra("WORKOUT_NAME", workoutName)
            putExtra("WORKOUT_DATE", workoutDate)
            putExtra("USER_ID", userId)
        }
        startActivity(intent)
        finish()
    }



}
