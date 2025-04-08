package com.example.test_techonstrelka

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.MainScope

class ChooseActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose)
        val day = findViewById<Button>(R.id.button_day)
        val week = findViewById<Button>(R.id.button_week)
        val month = findViewById<Button>(R.id.button_month)

        day.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).putExtra("MODE", 0))
        }
        week.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).putExtra("MODE", 1))
        }
        month.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).putExtra("MODE", 2))
        }
    }
}