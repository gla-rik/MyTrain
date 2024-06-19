package com.example.mytrain

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity2 : AppCompatActivity() {

    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        val sp = getSharedPreferences("TY", Context.MODE_PRIVATE)
        sp.edit().putString("TY", "9").apply()

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.nav_fragment1 -> selectedFragment = Calendar()
                R.id.nav_fragment3 -> selectedFragment = Workouts()
                R.id.nav_fragment5 -> selectedFragment = Profile()
            }
            if (selectedFragment != null) {
                currentFragment = selectedFragment
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit()
            }
            true
        }

        // Set default fragment
        if (savedInstanceState == null) {
            currentFragment = Calendar()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, currentFragment!!)
                .commit()
        }
    }

    override fun onBackPressed() {
        when (currentFragment) {
            is Workouts, is Profile -> {
                currentFragment = Calendar()
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, currentFragment!!)
                    .commit()
            }
            is Calendar -> {
                // Minimize the app
                moveTaskToBack(true)
            }
            else -> super.onBackPressed()
        }
    }
}
