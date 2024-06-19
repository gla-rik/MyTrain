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

class EditWorkoutActivity : AppCompatActivity() {

    private lateinit var workoutNameTextView: TextView
    private lateinit var editWorkoutNameButton: ImageButton
    private lateinit var addBlockButton: Button
    private lateinit var blocksLayout: LinearLayout
    private lateinit var db: FirebaseFirestore
    private var workoutId: String? = null
    private var workoutName: String? = null
    private var userId: String? = null
    private lateinit var deleteWorkoutButton: Button  // Новое поле для кнопки "Удалить тренировку"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_workout)

        workoutNameTextView = findViewById(R.id.workoutNameTextView)
        editWorkoutNameButton = findViewById(R.id.editWorkoutNameButton)
        addBlockButton = findViewById(R.id.addBlockButton)
        deleteWorkoutButton = findViewById(R.id.deleteWorkoutButton)  // Инициализация кнопки "Удалить тренировку"
        blocksLayout = findViewById(R.id.blocksLayout)
        db = Firebase.firestore

        workoutId = intent.getStringExtra("WORKOUT_ID")
        workoutName = intent.getStringExtra("WORKOUT_NAME")
        val blockId = intent.getStringExtra("BLOCK_ID")
        val blockName = intent.getStringExtra("BLOCK_NAME")

        Log.d("EditWorkoutActivity", "Получена тренировка: ID = $workoutId, Название = $workoutName, Блок: ID = $blockId, Название = $blockName")

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

        loadUserId {
            loadBlocks()
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
            val userWorkoutsCollection = db.collection("users").document(userId!!).collection("users_workouts")

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
            val blocksCollection = db.collection("users").document(userId!!).collection("users_workouts").document(workoutId!!).collection("blocks")

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
            val blocksCollection = db.collection("users").document(userId!!).collection("users_workouts").document(workoutId!!).collection("blocks")

            blocksCollection.orderBy("count").get()
                .addOnSuccessListener { documents ->
                    blocksLayout.removeAllViews()
                    for (document in documents) {
                        val blockName = document.getString("name")
                        val countString = document.getString("count")
                        val count = countString?.toIntOrNull() ?: 0  // Преобразование строки в Int
                        if (blockName != null) {
                            Log.d("EditWorkoutActivity", "Загружен блок: $blockName с count: $count")
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
                val intent = Intent(this@EditWorkoutActivity, SelectGroupActivity::class.java).apply {
                    putExtra("WORKOUT_ID", workoutId)
                    putExtra("WORKOUT_NAME", workoutName)
                    putExtra("BLOCK_ID", blockId)
                    putExtra("BLOCK_NAME", blockName)
                    putExtra("COLLECTION_TYPE", "users_workouts")
                }
                startActivity(intent)
            }
        }

        loadExercisesForBlock(blockId, exercisesLayout)

        blockLayout.addView(addExerciseButton)
        blocksLayout.addView(blockLayout)
    }


    private fun loadExercisesForBlock(blockId: String, exercisesLayout: LinearLayout) {
        val blockDocRef = db.collection("users").document(userId!!).collection("users_workouts").document(workoutId!!).collection("blocks").document(blockId)

        Log.d("EditWorkoutActivity", "Block reference: ${blockDocRef.path}")

        blockDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val blockData = mutableMapOf<String, Any?>()
                blockData["name"] = document.getString("name")
                blockData["exercises"] = document.get("exercises") as? MutableList<Map<String, Any>>

                Log.d("EditWorkoutActivity", "Block data: $blockData")

                val exercises = blockData["exercises"] as? MutableList<Map<String, Any>>
                if (exercises != null) {
                    exercisesLayout.removeAllViews()

                    for ((exerciseIndex, exercise) in exercises.withIndex()) {
                        val exerciseReference = exercise["exercise_reference"] as? String
                        if (exerciseReference != null) {
                            Log.d("EditWorkoutActivity", "Exercise reference: $exerciseReference")

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
                                                Log.d("EditWorkoutActivity", "Загружено упражнение: $exerciseName для блока: $blockId")

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
                                                        val intent = Intent(this@EditWorkoutActivity, ExerciseInfoActivity::class.java).apply {
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

                                                exercisesLayout.addView(exerciseLayout)
                                            } else {
                                                Log.e("EditWorkoutActivity", "Ошибка извлечения имени упражнения: индекс упражнения неверный или данные отсутствуют")
                                            }
                                        } else {
                                            Log.e("EditWorkoutActivity", "Документ упражнения не существует: ${exerciseRef.path}")
                                        }
                                    }.addOnFailureListener { e ->
                                        Log.e("EditWorkoutActivity", "Ошибка загрузки деталей упражнения: ${exerciseRef.path}", e)
                                    }
                                } else {
                                    Log.e("EditWorkoutActivity", "Индекс упражнения неверный: $exerciseReference")
                                }
                            } else {
                                Log.e("EditWorkoutActivity", "Ссылка на упражнение имеет неверный формат: $exerciseReference")
                            }
                        } else {
                            Log.d("EditWorkoutActivity", "Упражнение без ссылки или подходов в блоке: $blockId")
                        }
                    }
                } else {
                    Log.d("EditWorkoutActivity", "Нет упражнений для блока: $blockId")
                }
            } else {
                Log.e("EditWorkoutActivity", "Документ блока не существует: $blockId")
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Ошибка загрузки упражнений: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("EditWorkoutActivity", "Ошибка загрузки упражнений для блока: $blockId", e)
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

    private fun deleteExerciseFromFirestore(blockId: String, exerciseIndex: Int) {
        if (userId != null && workoutId != null) {
            val blockDocRef = db.collection("users")
                .document(userId!!)
                .collection("users_workouts")
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
            val userWorkoutsCollection = db.collection("users").document(userId!!).collection("users_workouts")

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
}
