package com.example.test_techonstrelka

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.test_techonstrelka.customview.TetrisView
import com.example.test_techonstrelka.datarepo.TaskRepository
import com.example.test_techonstrelka.models.ElementModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var tetrisView: TetrisView
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
            0 -> Pair(20, 12)
            1 -> Pair(12, 7)
            2 -> Pair(6, 5)
            else -> Pair(20, 10)
        }

        tetrisView.setGridSize(columns, rows)
        tetrisView.setGameOverListener {
            runOnUiThread {
                showGameOverDialog(this)
            }
        }


        val btn = findViewById<ImageButton>(R.id.imageButton2)
            val elements = database.getAllTasks().map {
                ElementModel(it.id, it.blockForm.toString())
            }
            tetrisView.activeElements.addAll(elements)
            tetrisView.startGame()

        tetrisView.setElementRequestListener {
            showAddDialog()
        }
        btn.setOnClickListener {
            showAddDialog()
        }
        tetrisView.setLineFilledListener { lineNumber ->
            showCongratilationDialog()
            Toast.makeText(this, "Строка $lineNumber заполнена!", Toast.LENGTH_SHORT).show()
        }
        tetrisView.setOnElementClickListener { elementId ->
            Toast.makeText(this, "Clicked element ID: $elementId", Toast.LENGTH_SHORT).show()
            showInfoDialog(elementId)

        }
    }

    private fun showGameOverDialog(context: Context) {
        try {
            val gameOverArray = arrayOf("Попробуйте еще раз!", "В следующий раз точно получится!", "Может быть потом повезет!").random()
            tetrisView.pauseGame()
            val congl = TextView(this).apply {
                textSize = 16f
                text = gameOverArray
            }
            val dialogBuilder = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                .setTitle("Вы проиграли!")
                .setView(congl)
                .setPositiveButton("Закрыть") { _, _ ->
                    startActivity(Intent(this, ChooseActivity::class.java))
                }
                .setOnDismissListener {
                    tetrisView.resumeGame()
                    startActivity(Intent(this, ChooseActivity::class.java))
                }
            dialogBuilder.show()
        } catch (e: Exception) {

        }
    }

    private fun showCongratilationDialog() {
        try {
            val conglArray = arrayOf("У тебя мать сдохла", "Ты долбаеб!", "Учись играть свинья!").random()
            tetrisView.pauseGame()
            val congl = TextView(this).apply {
                textSize = 16f
                text = conglArray


            }
            val dialogBuilder = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                .setTitle("Поздравляем!")
                .setView(congl)
                .setPositiveButton("Закрыть") { _, _ ->
                    tetrisView.resumeGame()
                }
                .setOnDismissListener {
                    tetrisView.resumeGame()
                }
            dialogBuilder.show()
        } catch (e: Exception) {

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



            val dialogView = LayoutInflater.from(this).inflate(R.layout.info_dialog, null)
            val infoTextView = dialogView.findViewById<TextView>(R.id.infoText)
            val closeButton = dialogView.findViewById<Button>(R.id.buttonClose)
            val deleteButton = dialogView.findViewById<Button>(R.id.buttonDelete)
            val timerButton = dialogView.findViewById<Button>(R.id.buttonTimer)
            val time = element.time.toDouble()/2
            infoTextView.text = """
            Название: ${element.name}
            Описание: ${element.description}
            Уровень важности: ${element.level}
            Категория: ${element.category}
            Время выполнения: ${time} часов
            """.trimIndent()

            val dialog = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Roundeddd)
                .setView(dialogView)
                .create()

            closeButton.setOnClickListener {
                tetrisView.resumeGame()
                dialog.dismiss()
            }

            deleteButton.setOnClickListener {
                database.deleteTask(elementId)
                tetrisView.removeElement(elementId)
                tetrisView.resumeGame()
                dialog.dismiss()
            }

            if (element.level == 2) {
                timerButton.visibility = View.VISIBLE
                timerButton.setOnClickListener {
                    val intent = Intent(this, PomodoroActivity::class.java)
                    intent.putExtra("name", elementId)
                    startActivity(intent)
                    dialog.dismiss()
                }
            }

            dialog.setOnDismissListener {
                tetrisView.resumeGame()
            }

            dialog.show()

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


            val dialogView = layoutInflater.inflate(R.layout.add_dialog, null).apply {
                // Убедимся, что макет заполняет весь доступный размер
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val dialog = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                .setView(dialogView)
                .setOnDismissListener {
                    isAddDialogShowing = false
                    tetrisView.resumeGame()
                }
                .create()
            dialog.window?.apply {
                // Устанавливаем флаги для фиксированного размера
                setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                )

                // Явно устанавливаем размеры и параметры
                setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                setBackgroundDrawableResource(android.R.color.transparent)

                // Ключевая настройка - предотвращаем изменение размера
            }

            dialog.setOnShowListener {
                val spinner: Spinner = dialogView.findViewById(R.id.taskTypeSpinner)
                val catspinner: Spinner = dialogView.findViewById(R.id.cathegoryTypeSpinner)
                val types = listOf("Обычная задача", "Важная задача")
                val cattypes = listOf("Работа", "Учеба", "Личные дела")
                val adapter = ArrayAdapter(this, R.layout.spinner_item_white, types).apply {
                    setDropDownViewResource(R.layout.spinner_dropdown_item_white)
                }
                val catadapter = ArrayAdapter(this, R.layout.spinner_item_white, cattypes).apply {
                    setDropDownViewResource(R.layout.spinner_dropdown_item_white)
                }
                spinner.adapter = adapter
                catspinner.adapter = catadapter

                val btnToGame = dialogView.findViewById<Button>(R.id.buttontogame)
                val btnAddMore = dialogView.findViewById<ImageButton>(R.id.imageButtonAddMore)
                val btnDelete = dialogView.findViewById<ImageButton>(R.id.DeleteAll)
                val seekBarValueText = dialogView.findViewById<TextView>(R.id.textView9)
                val inputHours = dialogView.findViewById<SeekBar>(R.id.seekBar)
                inputHours.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        val hoursCount = (progress+1)/2.0
                        seekBarValueText.text = "${hoursCount.toString() } часов"
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
                btnToGame.setOnClickListener {
                    val name = dialogView.findViewById<EditText>(R.id.editTextText).text.toString()
                    val description = dialogView.findViewById<EditText>(R.id.editTextText2).text.toString()
                    val hours = inputHours.progress + 1
                    val level = if (spinner.selectedItemPosition == 0) 1 else 2
                    val blockform = hours
                    val cathegory = catspinner.selectedItem.toString()
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

                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                    }
                }
                btnAddMore.setOnClickListener {
                    val name = dialogView.findViewById<EditText>(R.id.editTextText).text.toString()
                    val description = dialogView.findViewById<EditText>(R.id.editTextText2).text.toString()
                    val hours = dialogView.findViewById<SeekBar>(R.id.seekBar).progress + 1
                    val level = if (spinner.selectedItemPosition == 0) 1 else 2
                    val blockform = hours
                    val cathegory = catspinner.selectedItem.toString()
                    val id = UUID.randomUUID().toString()
                    if (name.isNotEmpty() && description.isNotEmpty() && hours.toString().isNotEmpty() && cathegory.isNotEmpty()) {
                        if (!tetrisView.isPaused){
                            tetrisView.pauseGame()
                        }
                        database.addTask(id, name, description, level, cathegory, hours.toString(), blockform)
                        val newElement = ElementModel(id, blockform.toString())
                        tetrisView.addNewElement(newElement)
                        dialogView.findViewById<EditText>(R.id.editTextText).text.clear()
                        dialogView.findViewById<EditText>(R.id.editTextText2).text.clear()
                        dialogView.findViewById<SeekBar>(R.id.seekBar).progress = 0
                        isAddDialogShowing = false

                        showAddDialog()
                    } else {
                        Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                    }
                }
                btnDelete.setOnClickListener {

                }
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