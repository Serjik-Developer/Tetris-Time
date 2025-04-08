package com.example.test_techonstrelka

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.test_techonstrelka.customview.TetrisView
import com.example.test_techonstrelka.datarepo.TaskRepository
import com.example.test_techonstrelka.models.ElementModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var tetrisView: TetrisView
    private lateinit var startButton: Button
    private lateinit var scoreText: TextView
    private lateinit var database: TaskRepository

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        database = TaskRepository(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tetrisView = findViewById(R.id.tetrisView)
        startButton = findViewById(R.id.startButton)
        scoreText = findViewById(R.id.scoreText)
        val btn = findViewById<Button>(R.id.addButton)
        startButton.setOnClickListener {
            val elements = database.getAllTasks().map {
                ElementModel(it.id, it.blockForm.toString())
            }
            tetrisView.activeElements.addAll(elements)
            tetrisView.startGame()
            startButton.visibility = View.GONE
        }
        tetrisView.setElementRequestListener {
            showAddDialog()
        }
        tetrisView.setScoreListener { score ->
            scoreText.text = "Score: $score"
        }
        btn.setOnClickListener {
            showAddDialog()
        }


    }
    private fun showAddDialog() {
        try {
            tetrisView.pauseGame()
            val inputName = EditText(this).apply {
                hint = "Введите название дела"
            }

            val inputDesc = EditText(this).apply {
                hint = "Введите описание дела"
            }
            val inputHours = EditText(this).apply {
                hint = "Введите количество часов"
            }
            val inputLevel = EditText(this).apply {
                hint = "Введите уровень важности"
            }
            val inputCathegory = EditText(this).apply {
                hint = "Введите категорию"
            }

            MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                .setTitle("Создать дело")
                .setView(LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(50, 20, 50, 20)
                    addView(inputName)
                    addView(inputDesc)
                    addView(inputHours)
                    addView(inputLevel)
                    addView(inputCathegory)
                })
                .setPositiveButton("Сохранить и играть") { _, _ ->
                    val name = inputName.text.toString()
                    val description = inputDesc.text.toString()
                    val hours = inputHours.text.toString()
                    val level = inputLevel.text.toString().toInt()
                    val blockform = hours.toInt()
                    val cathegory = inputCathegory.text.toString()
                    val id = UUID.randomUUID().toString()
                    database.addTask(id, name, description, level, cathegory, hours, blockform)
                    val newElement = ElementModel(id, blockform.toString())
                    tetrisView.addNewElement(newElement)
                    tetrisView.resumeGame()

                }
                .setNegativeButton("Сохранять дальше") { _, _ ->
                    val name = inputName.text.toString()
                    val description = inputDesc.text.toString()
                    val hours = inputHours.text.toString()
                    val level = inputLevel.text.toString().toInt()
                    val cathegory = inputCathegory.text.toString()
                    val blockform = hours.toInt()
                    val id = UUID.randomUUID().toString()
                    database.addTask(id, name, description, level, cathegory, hours, blockform)
                    val newElement = ElementModel(id, blockform.toString())
                    tetrisView.addNewElement(newElement)
                    showAddDialog()
                }
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Возникла ошибка!", Toast.LENGTH_LONG).show()
            Log.e("ERROR", e.toString())
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