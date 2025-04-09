package com.example.test_techonstrelka

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.media.Image
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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import com.example.test_techonstrelka.customview.TetrisView
import com.example.test_techonstrelka.datarepo.TaskRepository
import com.example.test_techonstrelka.models.ElementModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.ChoiceFormat
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private var dialog: AlertDialog? = null
    private lateinit var tetrisView: TetrisView
    private lateinit var database: TaskRepository
    private var isAddDialogShowing = false
    private var interval: Int = -1
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        database = TaskRepository(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tetrisView = findViewById(R.id.tetrisView)
        interval = intent.getIntExtra("INTERVAL", -1)
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
        val btnEsc = findViewById<ImageButton>(R.id.imageButton)
        btnEsc.setOnClickListener {
            startActivity(Intent(this, ChooseActivity::class.java))
        }

        val btn = findViewById<ImageButton>(R.id.imageButton2)
            val elements = database.getAllTasks(interval).map {
                ElementModel(it.id, it.blockForm.toString())
            }
            if (elements.size == 0) {
                showAddDialog()
            } else {
                tetrisView.activeElements.addAll(elements)
                tetrisView.startGame()

            }


        tetrisView.setElementRequestListener {
            showAddDialog()
        }
        btn.setOnClickListener {
            showAddDialog()
        }
        tetrisView.setLineFilledListener { lineNumber ->
            showCongratilationDialog()
        }
        tetrisView.setOnElementClickListener { elementId ->
            showInfoDialog(elementId)

        }
    }

    private fun showGameOverDialog(context: Context) {
        try {
            val gameOverMessages = arrayOf(
                "Попробуйте еще раз!",
                "В следующий раз точно получится!",
                "Может быть потом повезет!"
            )
            val randomMessage = gameOverMessages.random()

            tetrisView.pauseGame()

            val dialogView = LayoutInflater.from(this).inflate(R.layout.game_over_dialog, null).apply {
                findViewById<TextView>(R.id.gameOverMessage).text = randomMessage
            }

            val dialog = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_Rounded)
                .setView(dialogView)
                .setOnDismissListener {
                    tetrisView.resumeGame()
                    startActivity(Intent(this, ChooseActivity::class.java))
                }
                .create()

            dialogView.findViewById<Button>(R.id.closeButton).setOnClickListener {
                dialog.dismiss()
                startActivity(Intent(this, ChooseActivity::class.java))
            }

            dialog.window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
            }

            dialog.show()

        } catch (e: Exception) {
            Log.e("MAIN-ERROR", "Error: ${e.message}")
            startActivity(Intent(this, ChooseActivity::class.java))
        }
    }

    private fun showCongratilationDialog() {
        try {
            val tips = mapOf(
                "Планируй день заранее" to "Составь список задач накануне вечером",
                "Ставь приоритеты" to "Выделяй самые важные задачи и выполняй их первыми",
                "Минимизируй отвлекающие факторы" to "Отключи уведомления и сосредоточься на работе",
                "Периодически оценивай прогресс" to "Периодически оценивай прогресс"
            )

            val randomTip = tips.entries.random()

            tetrisView.pauseGame()

            val dialogView = LayoutInflater.from(this).inflate(R.layout.congrat_dialog, null).apply {
                findViewById<TextView>(R.id.tipTitle).text = randomTip.key
                findViewById<TextView>(R.id.tipDescription).text = randomTip.value
            }

            val dialog = MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog_RoundedCongl)
                .setView(dialogView)
                .setOnDismissListener {
                    tetrisView.resumeGame()
                }
                .create()

            dialogView.findViewById<Button>(R.id.closeButton).setOnClickListener {
                dialog.dismiss()
            }

            dialog.window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
            }

            dialog.show()

        } catch (e: Exception) {
            tetrisView.resumeGame()
            Log.e("MAIN-ERROR", "Error: ${e.message}")
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

            when (interval) {
                0 -> {
                    val time = element.time.toDouble()/2
                    infoTextView.text = """
            Название: ${element.name}
            Описание: ${element.description}
            Уровень важности: ${element.level}
            Категория: ${element.category}
            Время выполнения: ${time} часов
            """.trimIndent()

                }
                1 -> {
                    val time = element.time.toDouble()*2
                    infoTextView.text = """
            Название: ${element.name}
            Описание: ${element.description}
            Уровень важности: ${element.level}
            Категория: ${element.category}
            Время выполнения: ${time} часов
            """.trimIndent()
                }
                2 -> {
                    val time = element.time.toString()
                    infoTextView.text = """
            Название: ${element.name}
            Описание: ${element.description}
            Уровень важности: ${element.level}
            Категория: ${element.category}
            Время выполнения: ${time} суток
            """.trimIndent()
                }
            }

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
                    if (interval==0) intent.putExtra("IsDay", true)
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
            Log.e("MAIN-ERROR", "Error: ${e.message}")
            tetrisView.resumeGame()
        }
    }



    private fun showAddDialog() {

        if (isAddDialogShowing || dialog?.isShowing == true) return // Добавляем проверку на isShowing
        try {
            isAddDialogShowing = true
            tetrisView.pauseGame()


            val dialogView = layoutInflater.inflate(R.layout.add_dialog, null).apply {
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
                val textView7 = dialogView.findViewById<TextView>(R.id.textView7)
                val textView8 = dialogView.findViewById<TextView>(R.id.textView8)
                when (interval) {
                    0 -> {
                        inputHours.max = 11
                        textView7.text = "30 мин"
                        textView8.text = "6 часов"
                    }
                    1 -> {
                        inputHours.max = 11
                        textView7.text = "2 часа"
                        textView8.text = "24 часа"
                    }
                    2 -> {
                        inputHours.max = 4
                        textView7.text = "1 сутки"
                        textView8.text = "5 суток"
                    }
                }

                inputHours.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        // Вычисляем значение в зависимости от interval
                        when (interval) {
                            0 -> {
                                val hours = (progress + 1) / 2.0
                                seekBarValueText.text = "${hours} часов"
                            }
                            1 -> {
                                val hours = (progress + 1) * 2
                                seekBarValueText.text = "${hours} часов"
                            }
                            2 -> {
                                val days = progress + 1
                                seekBarValueText.text = if (days == 1) "1 сутки" else "$days суток"
                            }
                        }
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
                        database.addTask(id, name, description, level, cathegory, hours.toString(), blockform, interval)
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
                        database.addTask(id, name, description, level, cathegory, hours.toString(), blockform, interval)
                        val newElement = ElementModel(id, blockform.toString())
                        tetrisView.addNewElement(newElement)
                        dialogView.findViewById<EditText>(R.id.editTextText).text.clear()
                        dialogView.findViewById<EditText>(R.id.editTextText2).text.clear()
                        dialogView.findViewById<SeekBar>(R.id.seekBar).progress = 0
                        isAddDialogShowing = false


                    } else {
                        Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                    }
                }
                btnDelete.setOnClickListener {
                    dialog.dismiss()
                }
            }
            dialog.show()
        } catch (e: Exception) {
            isAddDialogShowing = false
            Toast.makeText(this, "Возникла ошибка!", Toast.LENGTH_LONG).show()
            Log.e("MAIN-ERROR", e.toString())
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