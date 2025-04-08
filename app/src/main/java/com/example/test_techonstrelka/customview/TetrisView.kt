package com.example.test_techonstrelka.customview



import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.example.test_techonstrelka.datarepo.TaskRepository
import com.example.test_techonstrelka.models.ElementModel

class TetrisView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    val database = TaskRepository(context)
    private val paint = Paint()
    private val cellSize = 50f
    private val gridWidth = 10
    private val gridHeight = 20
    private val grid = Array(gridWidth) { IntArray(gridHeight) }
    private var currentPiece: Piece? = null
    private var nextX = 0
    private var nextY = 0
    private var score = 0
    private var scoreListener: ((Int) -> Unit)? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateDelay = 500L // milliseconds
    private var isPaused = false
    val activeElements = mutableListOf<ElementModel>()
    private val placedElements = mutableListOf<ElementModel>()
    private var currentElement: ElementModel? = null
    private var elementRequestListener: (() -> Unit)? = null


    private fun getPiece (blockForm: Int) : Array<IntArray> {
        var Array: Array<IntArray> = arrayOf()
        when(blockForm) {
            1 -> Array = arrayOf(intArrayOf(1, 1, 1, 1)) // I
            2 -> Array = arrayOf(intArrayOf(1, 1), intArrayOf(1, 1)) // O
            3 -> Array = arrayOf(intArrayOf(0, 1, 0), intArrayOf(1, 1, 1)) // T
            4 -> Array =  arrayOf(intArrayOf(1, 1, 0), intArrayOf(0, 1, 1)) // Z
            5 -> Array = arrayOf(intArrayOf(0, 1, 1), intArrayOf(1, 1, 0)) // S
            6 -> Array =  arrayOf(intArrayOf(1, 0, 0), intArrayOf(1, 1, 1)) // L
            7 -> Array =  arrayOf(intArrayOf(0, 0, 1), intArrayOf(1, 1, 1))  // J
        }
        return Array
    }

    private val colors = arrayOf(
        Color.CYAN, Color.YELLOW, Color.MAGENTA,
        Color.RED, Color.GREEN, Color.BLUE, Color.parseColor("#FFA500")
    )

    private inner class Piece(val shape: Array<IntArray>, val color: Int) {
        var x = gridWidth / 2 - shape[0].size / 2
        var y = 0

        fun rotate(): Piece {
            val rows = shape.size
            val cols = shape[0].size
            val newShape = Array(cols) { IntArray(rows) }

            for (i in 0 until rows) {
                for (j in 0 until cols) {
                    newShape[j][rows - 1 - i] = shape[i][j]
                }
            }

            return Piece(newShape, color)
        }
    }
    fun setElementRequestListener(listener: () -> Unit) {
        elementRequestListener = listener
    }
    fun addNewElement(element: ElementModel) {
        activeElements.add(element)
        if (currentPiece == null && !isPaused) {
            spawnPiece()
            // Force a redraw and restart the update loop if needed
            invalidate()
            if (!handler.hasCallbacks(updateRunnable)) {
                handler.post(updateRunnable)
            }
        }
    }
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isPaused && currentPiece != null && !moveDown()) {
                placePiece()
                clearLines()
                spawnPiece()
                if (isGameOver()) {
                    handler.removeCallbacks(this)
                    return
                }
            }
            invalidate()
            if (!isPaused) {
                handler.postDelayed(this, updateDelay)
            }
        }
    }

    fun startGame() {
        score = 0
        scoreListener?.invoke(score)
        for (i in 0 until gridWidth) {
            for (j in 0 until gridHeight) {
                grid[i][j] = 0
            }
        }
        spawnPiece()
        handler.post(updateRunnable)
    }

    fun setScoreListener(listener: (Int) -> Unit) {
        scoreListener = listener
    }

    private fun spawnPiece() {
        if (activeElements.isEmpty()) {
            elementRequestListener?.invoke()
            return
        }
        currentElement = activeElements.first()
        try {
            val shape = getPiece(currentElement!!.blockForm.toInt())
            currentPiece = Piece(shape, colors.random())
            nextX = gridWidth / 2 - currentPiece!!.shape[0].size / 2
            nextY = 0
        } catch (e: Exception) {
            // Handle potential number format exceptions
            Log.e("TetrisView", "Error creating piece: ${e.message}")
            activeElements.remove(currentElement)
            currentElement = null
            if (activeElements.isNotEmpty()) {
                spawnPiece() // Try with next element
            } else {
                elementRequestListener?.invoke()
            }
        }
    }

    private fun placePiece() {
        val piece = currentPiece ?: return
        val element = currentElement ?: return

        for (i in 0 until piece.shape.size) {
            for (j in 0 until piece.shape[0].size) {
                if (piece.shape[i][j] == 1) {
                    val x = piece.x + i
                    val y = piece.y + j
                    if (y >= 0) {
                        grid[x][y] = piece.color
                    }
                }
            }
        }

        // Move element from active to placed list
        activeElements.remove(element)
        placedElements.add(element)
        currentPiece = null
        currentElement = null

        clearLines()

        // Check if we need more elements
        if (activeElements.isEmpty()) {
            elementRequestListener?.invoke()
        } else {
            spawnPiece()
            invalidate() // Force immediate redraw
        }
    }

    private fun moveDown(): Boolean {
        return movePiece(0, 1)
    }

    private fun moveLeft(): Boolean {
        return movePiece(-1, 0)
    }

    private fun moveRight(): Boolean {
        return movePiece(1, 0)
    }

    private fun movePiece(dx: Int, dy: Int): Boolean {
        val piece = currentPiece ?: return false

        val newX = piece.x + dx
        val newY = piece.y + dy

        if (isValidPosition(piece.shape, newX, newY)) {
            piece.x = newX
            piece.y = newY
            return true
        }
        return false
    }

    private fun isValidPosition(shape: Array<IntArray>, x: Int, y: Int): Boolean {
        for (i in 0 until shape.size) {
            for (j in 0 until shape[0].size) {
                if (shape[i][j] == 1) {
                    val newX = x + i
                    val newY = y + j

                    if (newX < 0 || newX >= gridWidth || newY >= gridHeight) {
                        return false
                    }

                    if (newY >= 0 && grid[newX][newY] != 0) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun rotatePiece(): Boolean {
        val piece = currentPiece ?: return false
        val rotated = piece.rotate()

        if (isValidPosition(rotated.shape, piece.x, piece.y)) {
            currentPiece = rotated
            return true
        }
        return false
    }

    private fun clearLines() {
        var linesCleared = 0

        var j = gridHeight - 1
        while (j >= 0) {
            var isLineFull = true
            for (i in 0 until gridWidth) {
                if (grid[i][j] == 0) {
                    isLineFull = false
                    break
                }
            }

            if (isLineFull) {
                linesCleared++
                for (jj in j downTo 1) {
                    for (i in 0 until gridWidth) {
                        grid[i][jj] = grid[i][jj - 1]
                    }
                }
                for (i in 0 until gridWidth) {
                    grid[i][0] = 0
                }
                // В Kotlin нельзя использовать j++ в этом контексте
                // Вместо этого мы остаемся на той же позиции j
                // так как строки сдвинулись вниз
            } else {
                j-- // Переходим к следующей строке только если текущая не была полной
            }
        }

        if (linesCleared > 0) {
            score += when (linesCleared) {
                1 -> 100
                2 -> 300
                3 -> 500
                4 -> 800
                else -> 0
            }
            scoreListener?.invoke(score)
        }
    }

    private fun isGameOver(): Boolean {
        for (i in 0 until gridWidth) {
            if (grid[i][0] != 0) {
                return true
            }
        }
        return false
    }

    fun pauseGame() {
        isPaused = true
        handler.removeCallbacks(updateRunnable)
    }

    fun resumeGame() {
        if (isPaused) {
            isPaused = false
            // Always post the runnable when resuming, not just when currentPiece exists
            handler.post(updateRunnable)
        }
    }
    fun togglePause() {
        if (isPaused) {
            resumeGame()
        } else {
            pauseGame()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw grid
        for (i in 0 until gridWidth) {
            for (j in 0 until gridHeight) {
                paint.color = if (grid[i][j] != 0) grid[i][j] else Color.LTGRAY
                paint.style = Paint.Style.FILL
                canvas.drawRect(
                    i * cellSize,
                    j * cellSize,
                    (i + 1) * cellSize,
                    (j + 1) * cellSize,
                    paint
                )

                paint.color = Color.DKGRAY
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1f
                canvas.drawRect(
                    i * cellSize,
                    j * cellSize,
                    (i + 1) * cellSize,
                    (j + 1) * cellSize,
                    paint
                )
            }
        }

        // Draw current piece
        currentPiece?.let { piece ->
            for (i in 0 until piece.shape.size) {
                for (j in 0 until piece.shape[0].size) {
                    if (piece.shape[i][j] == 1) {
                        val x = piece.x + i
                        val y = piece.y + j

                        paint.color = piece.color
                        paint.style = Paint.Style.FILL
                        canvas.drawRect(
                            x * cellSize,
                            y * cellSize,
                            (x + 1) * cellSize,
                            (y + 1) * cellSize,
                            paint
                        )

                        paint.color = Color.DKGRAY
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(
                            x * cellSize,
                            y * cellSize,
                            (x + 1) * cellSize,
                            (y + 1) * cellSize,
                            paint
                        )
                    }
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = (gridWidth * cellSize).toInt()
        val height = (gridHeight * cellSize).toInt()
        setMeasuredDimension(width, height)
    }

    fun onLeft() {
        moveLeft()
        invalidate()
    }

    fun onRight() {
        moveRight()
        invalidate()
    }

    fun onRotate() {
        rotatePiece()
        invalidate()
    }

    fun onDrop() {
        while (moveDown()) {
            // Keep moving down until it can't anymore
        }
        invalidate()
    }
}