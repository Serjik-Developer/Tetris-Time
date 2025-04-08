package com.example.test_techonstrelka

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.test_techonstrelka.customview.TetrisView

class MainActivity : AppCompatActivity() {
    private lateinit var tetrisView: TetrisView
    private lateinit var startButton: Button
    private lateinit var scoreText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tetrisView = findViewById(R.id.tetrisView)
        startButton = findViewById(R.id.startButton)
        scoreText = findViewById(R.id.scoreText)

        startButton.setOnClickListener {
            tetrisView.startGame()
            startButton.visibility = View.GONE
        }

        tetrisView.setScoreListener { score ->
            scoreText.text = "Score: $score"
        }


    }
    // Добавьте эти методы в класс MainActivity
    fun onLeftClick(view: View) {
        tetrisView.onLeft()
    }

    fun onRightClick(view: View) {
        tetrisView.onRight()
    }

    fun onRotateClick(view: View) {
        tetrisView.onRotate()
    }

    fun onDropClick(view: View) {
        tetrisView.onDrop()
    }
}