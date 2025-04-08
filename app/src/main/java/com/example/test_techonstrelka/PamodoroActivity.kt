package com.example.test_techonstrelka

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AppCompatActivity
import com.example.test_techonstrelka.datarepo.TaskRepository
import com.google.android.material.card.MaterialCardView
import android.widget.Button
import android.widget.TextView

class PomodoroActivity : AppCompatActivity() {
//TODO - ПОСМОТРЕЛ НА РАБОТУ, КРУЖЕК НЕ УМЕНЬШАЕТСЯ, СИДЕЛ СМОТРЕЛ НЕСКОЛЬКО МИНУТ, ПО ОСТАЛЬНОМУ НОРМАЛЬНО, ВСЕ КОРЕКТНО ОТОБРАЖАЕТСЯ(ВРОДЕ) ПОДПРАВЬ КРУЖЕК
//TODO - КРУЖЕК ПОДПРАВИШЬ - ЦЕНЫ ТЕБЕ НЕ БУДЕТ, ЧТО Я ИМЕЮ ВВИДУ, ОН НЕ УМЕНЬШАЕТСЯ ПО ИСТЕЧЕНИЮ ВРЕМЕНИ, ЕЩЕ ОДИН МОМЕНТ СВЯЗАННЫЙ С БАЗОЙ ДАННЫХ ОБСУДИМ ЛИЧНО
    private var isWorkTime = true
    private var isRunning = false
    private lateinit var timer: CountDownTimer
    private val workTime = 25 * 60 * 1000L // 25 минут
    private val breakTime = 5 * 60 * 1000L // 5 минут

    private lateinit var startButton: Button
    private lateinit var timerText: TextView
    private lateinit var statusText: TextView
    private lateinit var timerCard: MaterialCardView
    private lateinit var resetButton: Button
    private lateinit var skipButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pamodoro)
        startButton = findViewById(R.id.startButton)
        timerText = findViewById(R.id.timerText)
        statusText = findViewById(R.id.statusText)
        timerCard = findViewById(R.id.timerCard)
        resetButton = findViewById(R.id.resetButton)
        skipButton = findViewById(R.id.skipButton)

        val repoTask = TaskRepository(this)
        val id = intent.getStringExtra("name")

        val taskName = findViewById<TextView>(R.id.problemTitleText)
        val problemDesc = findViewById<TextView>(R.id.problemDescriptionText)

        taskName.setText(repoTask.getTaskById(id)?.name)
        problemDesc.setText(repoTask.getTaskById(id)?.description)

        startButton.setOnClickListener {
            if (isRunning) pauseTimer() else startTimer()
        }

        resetButton.setOnClickListener {
            resetTimer()
        }

        skipButton.setOnClickListener {
            skipToNextPhase()
        }
    }

    private fun startTimer() {
        val totalTime = if (isWorkTime) workTime else breakTime

        timer = object : CountDownTimer(totalTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateTimerText(millisUntilFinished)
                updateProgress(millisUntilFinished, totalTime)
            }

            override fun onFinish() {
                switchPhase()
            }
        }.start()

        isRunning = true
        startButton.text = "Пауза"
        updateUI()
    }

    private fun updateTimerText(millisUntilFinished: Long) {
        val minutes = millisUntilFinished / 1000 / 60
        val seconds = millisUntilFinished / 1000 % 60
        timerText.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun updateProgress(millisLeft: Long, totalTime: Long) {
        val progress = millisLeft.toFloat() / totalTime.toFloat()
        val color = if (isWorkTime) {
            Color.HSVToColor(floatArrayOf(progress * 120f, 1f, 1f)) // От красного к желтому
        } else {
            Color.HSVToColor(floatArrayOf(progress * 120f + 120f, 1f, 1f)) // От зеленого к синему
        }
        timerCard.strokeColor = color
    }

    private fun pauseTimer() {
        timer.cancel()
        isRunning = false
        startButton.text = "Старт"
    }

    private fun resetTimer() {
        if (isRunning) timer.cancel()
        isWorkTime = true
        timerText.text = "25:00"
        isRunning = false
        startButton.text = "Старт"
        updateUI()
    }

    private fun skipToNextPhase() {
        if (isRunning) timer.cancel()
        switchPhase()
    }

    private fun switchPhase() {
        isWorkTime = !isWorkTime
        updateUI()
        if (isRunning) startTimer()
    }

    private fun updateUI() {
        if (isWorkTime) {
            statusText.text = "Режим: Работа (25 мин)"
            timerCard.strokeColor = Color.parseColor("#E53935") // Красный
        } else {
            statusText.text = "Режим: Отдых (5 мин)"
            timerCard.strokeColor = Color.parseColor("#4CAF50") // Зеленый
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRunning) {
            timer.cancel()
        }
    }
}