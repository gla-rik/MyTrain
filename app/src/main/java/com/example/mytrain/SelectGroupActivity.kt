package com.example.mytrain

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class SelectGroupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_group)

        val workoutId = intent.getStringExtra("WORKOUT_ID")
        val workoutName = intent.getStringExtra("WORKOUT_NAME")
        val blockId = intent.getStringExtra("BLOCK_ID")
        val blockName = intent.getStringExtra("BLOCK_NAME")
        val collectionType = intent.getStringExtra("COLLECTION_TYPE")

        Log.d("SelectGroupActivity", "Редактируется тренировка: ID = $workoutId, Название = $workoutName, Блок: ID = $blockId, Название = $blockName, COLLECTION_TYPE = $collectionType")

        val groupLayouts = listOf(
            Pair(findViewById<LinearLayout>(R.id.warmup), "Разминка"),
            Pair(findViewById<LinearLayout>(R.id.stretching), "Растяжка"),
            Pair(findViewById<LinearLayout>(R.id.cardio), "Кардио"),
            Pair(findViewById<LinearLayout>(R.id.functional), "Функциональная тренировка"),
            Pair(findViewById<LinearLayout>(R.id.forearms), "Предплечья"),
            Pair(findViewById<LinearLayout>(R.id.triceps), "Трицепс"),
            Pair(findViewById<LinearLayout>(R.id.biceps), "Бицепс"),
            Pair(findViewById<LinearLayout>(R.id.shoulders), "Плечи"),
            Pair(findViewById<LinearLayout>(R.id.chest), "Грудь"),
            Pair(findViewById<LinearLayout>(R.id.back), "Спина"),
            Pair(findViewById<LinearLayout>(R.id.abs), "Пресс"),
            Pair(findViewById<LinearLayout>(R.id.quadriceps), "Квадрицепсы"),
            Pair(findViewById<LinearLayout>(R.id.buttocks), "Ягодицы"),
            Pair(findViewById<LinearLayout>(R.id.hamstrings), "Бицепс бедра"),
            Pair(findViewById<LinearLayout>(R.id.calf), "Голени")
        )

        val groupIds = listOf(
            "group_warmup", "group_stretching", "group_cardio", "group_functional", "group_forearms", "group_triceps",
            "group_biceps", "group_shoulders", "group_chest", "group_back", "group_abs", "group_quadriceps",
            "group_buttocks", "group_biceps_legs", "group_lower_legs"
        )

        for ((index, groupLayout) in groupLayouts.withIndex()) {
            groupLayout.first.setOnClickListener {
                val intent = Intent(this, ExerciseListActivity::class.java)
                intent.putExtra("GROUP_ID", groupIds[index])
                intent.putExtra("GROUP_NAME", groupLayout.second)
                intent.putExtra("WORKOUT_ID", workoutId)
                intent.putExtra("WORKOUT_NAME", workoutName)
                intent.putExtra("BLOCK_ID", blockId)
                intent.putExtra("BLOCK_NAME", blockName)
                intent.putExtra("COLLECTION_TYPE", collectionType)
                startActivity(intent)
            }
        }
    }

    override fun onBackPressed() {
        val workoutId = intent.getStringExtra("WORKOUT_ID")
        val workoutName = intent.getStringExtra("WORKOUT_NAME")
        val blockId = intent.getStringExtra("BLOCK_ID")
        val blockName = intent.getStringExtra("BLOCK_NAME")

        val intent = Intent(this, EditWorkoutActivity::class.java).apply {
            putExtra("WORKOUT_ID", workoutId)
            putExtra("WORKOUT_NAME", workoutName)
            putExtra("BLOCK_ID", blockId)
            putExtra("BLOCK_NAME", blockName)
        }
        startActivity(intent)
        finish()
    }
}
