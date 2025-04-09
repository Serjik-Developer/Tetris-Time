package com.example.test_techonstrelka

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test_techonstrelka.customview.CircularProgressView
import com.example.test_techonstrelka.datarepo.TaskRepository

class PomodoroActivity : AppCompatActivity() {
    private var isWorkTime = true
    private var isRunning = false
    private lateinit var timer: CountDownTimer
    private val workTime = 25 * 60 * 1000L
    private val breakTime = 5 * 60 * 1000L
    private var timeLeftInMillis = workTime
    private var cyclesCompleted = 0
    private var maxCycles = 0
    private var isTimeExpired = false

    private lateinit var startButton: Button
    private lateinit var timerText: TextView
    private lateinit var statusText: TextView
    private lateinit var resetButton: Button
    private lateinit var skipButton: Button
    private lateinit var progressView: CircularProgressView
    private lateinit var timeTask: TextView
    private lateinit var backButton: ImageButton
    private lateinit var addMoreTime: ImageView
    private lateinit var repoTask: TaskRepository
    private var isDay: Boolean = false
    private var id: String = ""

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pamodoro)
        startButton = findViewById(R.id.startButton)
        timerText = findViewById(R.id.timerText)
        statusText = findViewById(R.id.statusText)
        resetButton = findViewById(R.id.resetButton)
        skipButton = findViewById(R.id.skipButton)
        progressView = findViewById(R.id.progressView)
        timeTask = findViewById(R.id.taskTimeText)
        backButton = findViewById(R.id.backButton)
        addMoreTime = findViewById(R.id.addMoreTime)

        repoTask = TaskRepository(this)
        id = intent.getStringExtra("name")!!
        isDay = intent.getBooleanExtra("IsDay", false)
        maxCycles = repoTask.getTaskById(id)?.time?.toInt() ?: 0
        if (isDay) {
            val time = maxCycles.toDouble().div(2.0)
            timeTask.setText("Часы выполнения задачи: $time")
        }

        startButton.setOnClickListener {
            if (!isTimeExpired) {
                if (isRunning) pauseTimer() else startTimer()
            }
        }

        resetButton.setOnClickListener {
            if (!isTimeExpired) {
                resetTimer()
            }
        }

        skipButton.setOnClickListener {
            if (!isTimeExpired) {
                skipToNextPhase()
                if (!isRunning) {
                    startTimer()
                }
            }
        }

        backButton.setOnClickListener {
            returnToMainActivity()
        }
        addMoreTime.setOnClickListener {
            if (isTimeExpired) {
                addAdditionalCycle()
            }
        }

        updateUI()
        updateTimerText(timeLeftInMillis)
    }

    private fun addAdditionalCycle() {
        maxCycles++
        if (isDay) {
            val time = maxCycles.toDouble().div(2.0)
            timeTask.setText("Часы выполнения задачи: $time")
        }

        isTimeExpired = false
        startButton.isEnabled = true
        resetButton.isEnabled = true
        skipButton.isEnabled = true
        timerText.setTextColor(Color.WHITE)
        statusText.setTextColor(if (isWorkTime) Color.parseColor("#4CAF50") else Color.parseColor("#2196F3"))
        statusText.text = if (isWorkTime) "Режим: Работа (25 мин)" else "Режим: Отдых (5 мин)"
        if (!isRunning) {
            startTimer()
        }
    }

    private fun startTimer() {
        if (isTimeExpired) return

        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerText(millisUntilFinished)
                updateProgress(millisUntilFinished, if (isWorkTime) workTime else breakTime)
            }

            override fun onFinish() {
                timeLeftInMillis = 0
                if (isWorkTime) {
                    if (isDay) {
                        cyclesCompleted++
                    }
                    if (cyclesCompleted >= maxCycles && maxCycles > 0) {
                        Toast.makeText(this@PomodoroActivity, "Время вышло", Toast.LENGTH_LONG).show()
                        timeExpired()
                        return
                    }
                }
                switchPhase()
            }

        }.start()

        isRunning = true
        startButton.text = "Пауза"
        updateUI()
    }

    private fun timeExpired() {
        isTimeExpired = true
        startButton.isEnabled = false
        resetButton.isEnabled = false
        skipButton.isEnabled = false

        timerText.setTextColor(Color.RED)
        statusText.setTextColor(Color.RED)
        statusText.text = "Время истекло"
        AppData.idToRemove = id
        AppData.needsRefresh = true
        repoTask.deleteTask(id)
        try {
            repoTask.deleteTask(id)
        } catch (e: Exception) {
            Log.e("ERROR-MAIN", e.message.toString())
        }

    }

    private fun returnToMainActivity() {
        finish()
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
        cyclesCompleted = 0
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
        if (isWorkTime) {
            if (isDay) {
                cyclesCompleted++
            }
            if (cyclesCompleted >= maxCycles && maxCycles > 0) {
                Toast.makeText(this@PomodoroActivity, "Время вышло", Toast.LENGTH_LONG).show()
                timeExpired()
                return
            }
        }
        switchPhase()
    }

    private fun switchPhase() {
        isWorkTime = !isWorkTime
        timeLeftInMillis = if (isWorkTime) workTime else breakTime
        updateTimerText(timeLeftInMillis)
        progressView.setProgress(0f)
        updateUI()
    }

    private fun updateUI() {
        if (isWorkTime) {
            statusText.text = "Режим: Работа (25 мин)"
            statusText.setTextColor(Color.parseColor("#4CAF50"))
        } else {
            statusText.text = "Режим: Отдых (5 мин)"
            statusText.setTextColor(Color.parseColor("#2196F3"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRunning) {
            timer.cancel()
        }
    }
}