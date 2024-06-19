package com.example.mytrain

import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage

class ExerciseInfoActivity : AppCompatActivity() {

    private lateinit var exerciseNameTextView: TextView
    private lateinit var exerciseDescriptionTextView: TextView
    private lateinit var exerciseInventoryTextView: TextView
    private lateinit var exerciseGifImageView: ImageView
    private lateinit var progressBarLoadingGif: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercise_info)

        exerciseNameTextView = findViewById(R.id.textViewExerciseName)
        exerciseDescriptionTextView = findViewById(R.id.textViewExerciseDescription)
        exerciseInventoryTextView = findViewById(R.id.textViewExerciseInventory)
        exerciseGifImageView = findViewById(R.id.imageViewExerciseGif)
        progressBarLoadingGif = findViewById(R.id.progressBarLoadingGif)

        val exerciseName = intent.getStringExtra("EXERCISE_NAME")
        val exerciseDescription = intent.getStringExtra("EXERCISE_DESCRIPTION")
        val exerciseInventory = intent.getStringExtra("EXERCISE_INVENTORY")
        val exerciseMediaPath = intent.getStringExtra("EXERCISE_MEDIA_PATH")

        exerciseNameTextView.text = exerciseName
        exerciseDescriptionTextView.text = exerciseDescription
        exerciseInventoryTextView.text = exerciseInventory

        // Загрузка GIF из Firebase Storage
        if (exerciseMediaPath != null) {
            val storageReference = FirebaseStorage.getInstance().getReference(exerciseMediaPath)
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                if (!isDestroyed && !isFinishing) {
                    Glide.with(this)
                        .asGif()
                        .load(uri)
                        .into(exerciseGifImageView)
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
        }
    }
}
