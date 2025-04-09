package com.example.test_techonstrelka

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
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
    private var isAddDialogShowing = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        database = TaskRepository(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tetrisView = findViewById(R.id.tetrisView)

        val mode = intent.getIntExtra("MODE", -1)
        val (rows, columns) = when (mode) {
            0 -> Pair(10, 12)
            1 -> Pair(12, 7)
            2 -> Pair(6, 5)
            else -> Pair(20, 10)
        }

        tetrisView.setGridSize(columns, rows)


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
        btn.setOnClickListener {
            showAddDialog()
        }
        tetrisView.setLineFilledListener { lineNumber ->
            Toast.makeText(this, "Строка $lineNumber заполнена!", Toast.LENGTH_SHORT).show()
        }
        tetrisView.setOnElementClickListener { elementId ->
            Toast.makeText(this, "Clicked element ID: $elementId", Toast.LENGTH_SHORT).show()
            showInfoDialog(elementId)

        }
    }

    @SuppressLint("SetTextI18n")
    private fun showInfoDialog(elementId: String) {
        try {
            tetrisView.pauseGame()
            val element = database.getTaskById(elementId) ?: run {
                Toast.makeText(this, "Элемент не найден", Toast.LENGTH_SHORT).show()
                tetrisView.resumeGame()
                return
            }

            val infoText = TextView(this).apply {
                textSize = 16f
                setPadding(50, 30, 50, 30)
                text = """
            Название: ${element.name}
            Описание: ${element.description}
            Уровень важности: ${element.level}
            Категория: ${element.category}
            Время выполнения: ${element.time } часов
            Форма блока: ${when(element.blockForm) {
                    1 -> "I"
                    2 -> "O"
                    3 -> "T"
                    4 -> "Z"
                    5 -> "S"
                    6 -> "L"
                    7 -> "J"
                    else -> "Неизвестно"
                }}
            """.trimIndent()
            }

            val dialogBuilder = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                .setTitle("Информация о деле")
                .setView(infoText)
                .setPositiveButton("Закрыть") { _, _ ->
                    tetrisView.resumeGame()
                }.setNegativeButton("Удалить дело") { _, _ ->
                    database.deleteTask(elementId)
                    tetrisView.removeElement(elementId)
                    tetrisView.resumeGame()
                }
                .setOnDismissListener {
                    tetrisView.resumeGame()
                }


            if (element.level == 2) {
                dialogBuilder.setNeutralButton("Таймер") { _, _ ->
                    val intent = Intent(this, PomodoroActivity::class.java).apply {
                        putExtra("name", elementId)
                    }
                    startActivity(intent)
                }
            }

            dialogBuilder.show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка при отображении информации", Toast.LENGTH_SHORT).show()
            Log.e("INFO_DIALOG", "Error: ${e.message}")
            tetrisView.resumeGame()
        }
    }

    private fun showAddDialog() {

        if (isAddDialogShowing) return
        try {
            isAddDialogShowing = true
            tetrisView.pauseGame()
            val inputName = EditText(this).apply {
                hint = "Введите название дела"
            }

            val inputDesc = EditText(this).apply {
                hint = "Введите описание дела"
            }
            val inputHours = SeekBar(this).apply {
                max = 11
                progress = 0
            }
            val seekBarValueText = TextView(this).apply {
                text = "Выбрано часов: ${inputHours.progress}"
                textSize = 16f
                setPadding(0, 10, 0, 10)
            }
            seekBarValueText.text = "Выбрано часов: 1"
            inputHours.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    seekBarValueText.text = "Выбрано часов: ${progress+1}"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
            val importanceOptions = arrayOf("Обычная задача", "Важная задача")
            val importanceSpinner = android.widget.Spinner(this).apply {
                adapter = android.widget.ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    importanceOptions
                )
            }
            val cathegoryOptions = arrayOf("Работа", "Учеба", "Личные дела")
            val cathegorySpinner = android.widget.Spinner(this).apply {
                adapter = android.widget.ArrayAdapter(
                    this@MainActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    cathegoryOptions
                )
            }

            val dialog = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                .setTitle("Создать дело")
                .setView(LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(50, 20, 50, 20)
                    addView(inputName)
                    addView(inputDesc)
                    addView(inputHours)
                    addView(importanceSpinner)
                    addView(cathegorySpinner)
                    addView(seekBarValueText)
                })
                .setPositiveButton("Сохранить и играть") { dialogInterface, _ ->
                    val name = inputName.text.toString()
                    val description = inputDesc.text.toString()
                    val hours = inputHours.progress + 1

                    val level = if (importanceSpinner.selectedItemPosition == 0) 1 else 2
                    val blockform = hours
                    val cathegory = cathegorySpinner.selectedItem.toString()
                    val id = UUID.randomUUID().toString()

                    if (name.isNotEmpty() && description.isNotEmpty() && hours.toString().isNotEmpty() && cathegory.isNotEmpty()) {
                        database.addTask(id, name, description, level, cathegory, hours.toString(), blockform)
                        val newElement = ElementModel(id, blockform.toString())
                        tetrisView.addNewElement(newElement)
                        if (tetrisView.isPaused) {
                            tetrisView.resumeGame()
                        }
                        if (tetrisView.currentPiece == null) {
                            tetrisView.spawnPiece()
                            tetrisView.invalidate()
                        }

                        dialogInterface.dismiss()
                    } else {
                        Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Сохранять дальше") { _, _ ->

                    val name = inputName.text.toString()
                    val description = inputDesc.text.toString()
                    val hours = inputHours.progress + 1

                    val level = if (importanceSpinner.selectedItemPosition == 0) 1 else 2
                    val blockform = hours
                    val cathegory = cathegorySpinner.selectedItem.toString()
                    val id = UUID.randomUUID().toString()

                    if (name.isNotEmpty() && description.isNotEmpty() && hours.toString().isNotEmpty() && cathegory.isNotEmpty()) {
                        if (!tetrisView.isPaused){
                            tetrisView.pauseGame()
                        }
                        database.addTask(id, name, description, level, cathegory, hours.toString(), blockform)
                        val newElement = ElementModel(id, blockform.toString())
                        tetrisView.addNewElement(newElement)
                        inputName.text.clear()
                        inputDesc.text.clear()
                        inputHours.progress = 0
                        isAddDialogShowing = false

                        showAddDialog()
                    } else {
                        Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                    }
                }
                .setOnDismissListener {
                    isAddDialogShowing = false
                    tetrisView.resumeGame()
                }
                .create()

            dialog.setOnShowListener {
                inputName.text.clear()
                inputDesc.text.clear()
                inputHours.progress = 0
            }

            dialog.show()
        } catch (e: Exception) {
            isAddDialogShowing = false
            Toast.makeText(this, "Возникла ошибка!", Toast.LENGTH_LONG).show()
            Log.e("ERROR", e.toString())
            tetrisView.resumeGame()
        }
    }
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