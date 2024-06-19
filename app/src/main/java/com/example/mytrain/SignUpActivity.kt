package com.example.mytrain

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        val email: TextView = findViewById(R.id.email)
        val password: TextView = findViewById(R.id.password)
        val password1: TextView = findViewById(R.id.password1)  // для проверки пароля
        val weight: TextView = findViewById(R.id.weight)
        val height: TextView = findViewById(R.id.height)
        val genderRadioGroup: RadioGroup = findViewById(R.id.genderRadioGroup)

        val button: ConstraintLayout = findViewById(R.id.button)
        val button1: TextView = findViewById(R.id.signuptext)

        button1.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        button.setOnClickListener {
            val selectedGenderId = genderRadioGroup.checkedRadioButtonId
            val selectedGenderButton: RadioButton? = findViewById(selectedGenderId)
            val gender: String = when (selectedGenderButton?.text) {
                "мужской" -> "m"
                "женский" -> "f"
                else -> ""
            }
            val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{1,}$".toRegex()
            val emailText = email.text.toString()

            if (emailText.isEmpty() || !emailText.matches(emailPattern)) {
                Toast.makeText(this, "Проверьте поле email", Toast.LENGTH_LONG).show()
            } else if (password.text.isEmpty() || password.text.length < 6) {
                Toast.makeText(this, "Пароль должен быть больше 6 символов", Toast.LENGTH_LONG).show()
            } else if (password.text.toString() != password1.text.toString()) {
                Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_LONG).show()
            } else if (weight.text.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, введите вес", Toast.LENGTH_LONG).show()
            } else if (height.text.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, введите рост", Toast.LENGTH_LONG).show()
            } else if (gender.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, выберите пол", Toast.LENGTH_LONG).show()
            } else {
                val db = Firebase.firestore
                val hashedPassword = PasswordEncryptor.hashPassword(password.text.toString())
                val weightArray = listOf(weight.text.toString())
                val user = hashMapOf(
                    "email" to email.text.toString(),
                    "password" to hashedPassword,
                    "weight" to weightArray,
                    "height" to height.text.toString(),
                    "gender" to gender
                )

                db.collection("users")
                    .add(user)
                    .addOnSuccessListener { documentReference ->
                        // Не сохраняем email в SharedPreferences
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                        Toast.makeText(this, "Регистрация прошла успешно, пожалуйста, авторизуйтесь", Toast.LENGTH_LONG).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Регистрация не произошла", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }
}
