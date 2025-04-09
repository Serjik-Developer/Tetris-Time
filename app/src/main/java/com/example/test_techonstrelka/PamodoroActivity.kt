package com.example.test_techonstrelka

import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.test_techonstrelka.customview.CircularProgressView
import com.example.test_techonstrelka.datarepo.TaskRepository

class PomodoroActivity : AppCompatActivity() {
    private var isWorkTime = true
    private var isRunning = false
    private lateinit var timer: CountDownTimer
    private val workTime = 25 * 60 * 1000L
    private val breakTime = 5 * 60 * 1000L
    private var timeLeftInMillis = workTime // Текущее оставшееся время

    private lateinit var startButton: Button
    private lateinit var timerText: TextView
    private lateinit var statusText: TextView
    private lateinit var resetButton: Button
    private lateinit var skipButton: Button
    private lateinit var progressView: CircularProgressView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pamodoro)

        startButton = findViewById(R.id.startButton)
        timerText = findViewById(R.id.timerText)
        statusText = findViewById(R.id.statusText)
        resetButton = findViewById(R.id.resetButton)
        skipButton = findViewById(R.id.skipButton)
        progressView = findViewById(R.id.progressView)

        val repoTask = TaskRepository(this)
        val id = intent.getStringExtra("name")

        val taskName = findViewById<TextView>(R.id.problemTitleText)
        val problemDesc = findViewById<TextView>(R.id.problemDescriptionText)

        taskName.text = repoTask.getTaskById(id)?.name
        problemDesc.text = repoTask.getTaskById(id)?.description

        startButton.setOnClickListener {
            if (isRunning) pauseTimer() else startTimer()
        }

        resetButton.setOnClickListener {
            resetTimer()
        }

        skipButton.setOnClickListener {
            skipToNextPhase()
        }

        updateUI()
        updateTimerText(timeLeftInMillis)
    }

    private fun startTimer() {
        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText(millisUntilFinished)
                updateProgress(millisUntilFinished, if (isWorkTime) workTime else breakTime)
            }

            override fun onFinish() {
                timeLeftInMillis = 0
                switchPhase()
            }
        }.start()

        isRunning = true
        startButton.text = "Пауза"
        updateUI()
    }

    private fun updateProgress(millisLeft: Long, totalTime: Long) {
        val progress = 1f - (millisLeft.toFloat() / totalTime.toFloat())
        progressView.setProgress(progress)

        val hue = if (isWorkTime) {
            120f * (millisLeft.toFloat() / totalTime.toFloat())
        } else {
            240f - (120f * (millisLeft.toFloat() / totalTime.toFloat()))
        }
        val color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        statusText.setTextColor(color)
    }

    private fun updateTimerText(millisUntilFinished: Long) {
        val minutes = (millisUntilFinished / 1000) / 60
        val seconds = (millisUntilFinished / 1000) % 60
        timerText.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun pauseTimer() {
        timer.cancel()
        isRunning = false
        startButton.text = "Старт"
    }

    private fun resetTimer() {
        if (isRunning) {
            timer.cancel()
            isRunning = false
        }
        isWorkTime = true
        timeLeftInMillis = workTime
        updateTimerText(timeLeftInMillis)
        progressView.setProgress(0f)
        startButton.text = "Старт"
        updateUI()
    }

    private fun skipToNextPhase() {
        if (isRunning) {
            timer.cancel()
            isRunning = false
        }
        switchPhase()
    }

    private fun switchPhase() {
        isWorkTime = !isWorkTime
        timeLeftInMillis = if (isWorkTime) workTime else breakTime
        updateTimerText(timeLeftInMillis)
        progressView.setProgress(0f)
        updateUI()

        if (isRunning) {
            startTimer()
        }


    }

    private fun updateUI() {
        if (isWorkTime) {
            statusText.text = "Режим: Работа (25 мин)"
            statusText.setTextColor(Color.parseColor("#4CAF50")) // Зеленый
        } else {
            statusText.text = "Режим: Отдых (5 мин)"
            statusText.setTextColor(Color.parseColor("#2196F3")) // Синий
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRunning) {
            timer.cancel()
        }
    }
}