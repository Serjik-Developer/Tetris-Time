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
            Время выполнения: ${element.time} часов
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


            if (element.level == 2) { //ВРЕМЕНО ДОБАВЛЕН УРОВЕНЬ ВАЖНОСТИ 2, ПОЗЖЕ БУДЕТ ОБСУЖДАТЬСЯ КОНКРЕТНЫЙ
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
            val inputHours = EditText(this).apply {
                hint = "Введите количество часов"
                inputType = InputType.TYPE_CLASS_NUMBER
                filters = arrayOf(InputFilter.LengthFilter(3))
            }
            val inputLevel = EditText(this).apply {
                hint = "Введите уровень важности"
                inputType = InputType.TYPE_CLASS_NUMBER
                filters = arrayOf(
                    InputFilter.LengthFilter(1),
                    InputFilter { source, start, end, dest, dstart, dend ->
                        if (source.isNotEmpty()) {
                            val newValue = source.toString().toIntOrNull()
                            if (newValue != null && newValue in 1..2) {
                                null
                            } else {
                                ""
                            }
                        } else {
                            null
                        }
                    }
                )
            }
            val inputCathegory = EditText(this).apply {
                hint = "Введите категорию"
            }

            val dialog = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
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
                .setPositiveButton("Сохранить и играть") { dialogInterface, _ ->
                    val name = inputName.text.toString()
                    val description = inputDesc.text.toString()
                    val hours = inputHours.text.toString()
                    val level = inputLevel.text.toString().toInt()
                    val blockform = if (hours.isNotEmpty()) hours.toInt() else 0
                    val cathegory = inputCathegory.text.toString()
                    val id = UUID.randomUUID().toString()

                    if (name.isNotEmpty() && description.isNotEmpty() && hours.isNotEmpty() && cathegory.isNotEmpty()) {
                        database.addTask(id, name, description, level, cathegory, hours, blockform)
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
                    val hours = inputHours.text.toString()
                    val level = inputLevel.text.toString().toInt()
                    val blockform = if (hours.isNotEmpty()) hours.toInt() else 0
                    val cathegory = inputCathegory.text.toString()
                    val id = UUID.randomUUID().toString()

                    if (name.isNotEmpty() && description.isNotEmpty() && hours.isNotEmpty() && cathegory.isNotEmpty()) {
                        if (!tetrisView.isPaused){
                            tetrisView.pauseGame()
                        }
                        database.addTask(id, name, description, level, cathegory, hours, blockform)
                        val newElement = ElementModel(id, blockform.toString())
                        tetrisView.addNewElement(newElement)
                        inputName.text.clear()
                        inputDesc.text.clear()
                        inputHours.text.clear()
                        inputLevel.text.clear()
                        inputCathegory.text.clear()
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
                inputHours.text.clear()
                inputLevel.text.clear()
                inputCathegory.text.clear()
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