package com.example.mytrain

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutDay : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_day_activity) // Убедитесь, что у вас есть layout файл activity_new.xml

        // Получаем данные даты из Intent
        val date = intent.getStringExtra("selected_date")
        findViewById<TextView>(R.id.aboutDayText).text = date // Предполагается, что у вас есть TextView с id textView в activity_new.xml
    }
}