package com.example.test_techonstrelka

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.test_techonstrelka.datarepo.TaskRepository

class PamodoroActivity : AppCompatActivity() {

    private var isWorkTime = true
    private var isRunning = false
    private lateinit var timer: CountDownTimer
    private val workTime = 25 * 60 * 1000L // 25 минут
    private val breakTime = 5 * 60 * 1000L // 5 минут
    private val startButton = findViewById<Button>(R.id.startButton)
    private val timerText = findViewById<TextView>(R.id.timerText)
    private val statusText = findViewById<TextView>(R.id.statusText)


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pamodoro)

        val repoTask = TaskRepository(this)
        val id  = intent.getStringExtra("name")
        // Здесь можно получить данные задачи из Intent
        val taskName = findViewById<TextView>(R.id.taskNameText)
        val taskDesc = findViewById<TextView>(R.id.taskDescriptionText)

        taskName.setText(repoTask.getTaskById(id)?.name)
        taskDesc.setText(repoTask.getTaskById(id)?.description)

        startButton.setOnClickListener {
            if (isRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }
    }

    private fun startTimer() {
        val totalTime = if (isWorkTime) workTime else breakTime

        timer = object : CountDownTimer(totalTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                timerText.text = String.format("%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                isWorkTime = !isWorkTime
                updateUI()
                startTimer() // Автоматически запускаем следующий таймер
            }
        }.start()

        isRunning = true
        startButton.text = "Пауза"
        updateUI()
    }

    private fun pauseTimer() {
        timer.cancel()
        isRunning = false
        startButton.text = "Старт"
    }

    private fun updateUI() {
        if (isWorkTime) {
            timerText.setTextColor(Color.parseColor("#E53935")) // Красный
            statusText.text = "Режим: Работа (25 мин)"
        } else {
            timerText.setTextColor(Color.parseColor("#388E3C")) // Зеленый
            statusText.text = "Режим: Отдых (5 мин)"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRunning) {
            timer.cancel()
        }
    }





}