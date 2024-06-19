package com.example.mytrain

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.mindrot.jbcrypt.BCrypt

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sp = getSharedPreferences("TY", Context.MODE_PRIVATE)
        val savedEmail = sp.getString("Email", null)

        if (savedEmail != null) {
            // Пользователь уже авторизован
            handleAuthorizedUser()
            return
        }

        val email: TextView = findViewById(R.id.email)
        val password: TextView = findViewById(R.id.password)
        val button: ConstraintLayout = findViewById(R.id.button)
        val signuptext: TextView = findViewById(R.id.signuptext)  // Переместил сюда
        val db = Firebase.firestore

        button.setOnClickListener {
            db.collection("users")
                .whereEqualTo("email", email.text.toString())
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        Toast.makeText(this, "Email не найден", Toast.LENGTH_LONG).show()
                    } else {
                        val document = result.documents[0]
                        val storedPasswordHash = document.getString("password")
                        val userId = document.id // Получение USER_ID

                        if (storedPasswordHash != null && BCrypt.checkpw(password.text.toString(), storedPasswordHash)) {
                            sp.edit().apply {
                                putString("Email", email.text.toString())
                                putString("USER_ID", userId) // Сохранение USER_ID
                                apply()
                            }
                            handleAuthorizedUser()
                        } else {
                            Toast.makeText(this, "Неверный пароль", Toast.LENGTH_LONG).show()
                            password.text = ""
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_LONG).show()
                }
        }

        signuptext.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun handleAuthorizedUser() {
        val intent = Intent(this, MainActivity2::class.java)

        // Получаем данные, если они переданы
        val workoutId = intent.getStringExtra("WORKOUT_ID")
        val workoutName = intent.getStringExtra("WORKOUT_NAME")
        val workoutDate = intent.getStringExtra("WORKOUT_DATE")
        val userId = intent.getStringExtra("USER_ID")

        // Добавляем данные в интент
        intent.apply {
            putExtra("WORKOUT_ID", workoutId)
            putExtra("WORKOUT_NAME", workoutName)
            putExtra("WORKOUT_DATE", workoutDate)
            putExtra("USER_ID", userId)
        }

        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Закрыть приложение полностью при нажатии на кнопку "назад"
        finishAffinity()
    }
}
