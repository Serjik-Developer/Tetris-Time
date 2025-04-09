package com.example.test_techonstrelka.customview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.example.test_techonstrelka.datarepo.TaskRepository
import com.example.test_techonstrelka.models.ElementModel


class TetrisView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var gridWidth: Int = 10
    private var gridHeight: Int = 20
    private val paint = Paint()
    private val cellSize: Float
        get() = minOf(
            width.toFloat() / gridWidth,
            height.toFloat() / gridHeight
        )
    private var grid = Array(gridWidth) { IntArray(gridHeight) }
    private var elementGrid = Array(gridWidth) { Array<String?>(gridHeight) { null } }
    var currentPiece: Piece? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateDelay = 500L
    var isPaused = false
    val activeElements = mutableListOf<ElementModel>()
    private val placedElements = mutableListOf<ElementModel>()
    private var currentElement: ElementModel? = null
    private var elementRequestListener: (() -> Unit)? = null
    private var elementClickListener: ((String) -> Unit)? = null
    private var lineFilledListener: ((Int) -> Unit)? = null
    private var filledLines: MutableList<Int>? = mutableListOf()
    private var isRequestingElement = false

    init {
        grid = Array(gridWidth) { IntArray(gridHeight) }
        elementGrid = Array(gridWidth) { Array<String?>(gridHeight) { null } }
    }
    fun removeElement(elementId: String) {
        for (i in 0 until gridWidth) {
            for (j in 0 until gridHeight) {
                if (elementGrid[i][j] == elementId) {
                    grid[i][j] = 0
                    elementGrid[i][j] = null
                }
            }
        }
        placedElements.removeAll { it.id == elementId }
        applyGravity()
        invalidate()
    }
    private fun applyGravity() {
        for (i in 0 until gridWidth) {
            val columnElements = mutableListOf<Pair<Int, String>>()
            val columnColors = mutableListOf<Int>()

            for (j in 0 until gridHeight) {
                if (grid[i][j] != 0 && elementGrid[i][j] != null) {
                    columnElements.add(Pair(j, elementGrid[i][j]!!))
                    columnColors.add(grid[i][j])
                }
            }
            for (j in 0 until gridHeight) {
                grid[i][j] = 0
                elementGrid[i][j] = null
            }
            for ((index, pair) in columnElements.withIndex()) {
                val newJ = gridHeight - columnElements.size + index
                if (newJ >= 0) {
                    grid[i][newJ] = columnColors[index]
                    elementGrid[i][newJ] = pair.second
                }
            }
        }
    }

    private fun getPiece(blockForm: Int): Array<IntArray> {
        return when (blockForm) {
            1 -> arrayOf(intArrayOf(1, 1, 1, 1)) // I
            2 -> arrayOf(intArrayOf(1, 1), intArrayOf(1, 1)) // O
            3 -> arrayOf(intArrayOf(0, 1, 0), intArrayOf(1, 1, 1)) // T
            4 -> arrayOf(intArrayOf(1, 1, 0), intArrayOf(0, 1, 1)) // Z
            5 -> arrayOf(intArrayOf(0, 1, 1), intArrayOf(1, 1, 0)) // S
            6 -> arrayOf(intArrayOf(1, 0, 0), intArrayOf(1, 1, 1)) // L
            7 -> arrayOf(intArrayOf(0, 0, 1), intArrayOf(1, 1, 1)) // J
            8 -> arrayOf( // "Пушка" (крест с хвостиком)
                intArrayOf(0, 1, 0),
                intArrayOf(1, 1, 1),
                intArrayOf(0, 1, 0)
            )
            9 -> arrayOf( // "Точка с крыльями"
                intArrayOf(0, 1, 0),
                intArrayOf(1, 1, 1),
                intArrayOf(1, 0, 1)
            )
            10 -> arrayOf( // "Шаг"
                intArrayOf(1, 0, 0),
                intArrayOf(1, 1, 0),
                intArrayOf(0, 1, 1)
            )
            11 -> arrayOf( // "U"
                intArrayOf(1, 0, 1),
                intArrayOf(1, 1, 1)
            )
            12 -> arrayOf( // "Большой угол"
                intArrayOf(1, 0),
                intArrayOf(1, 0),
                intArrayOf(1, 1)
            )
            else -> arrayOf()
        }
    }


    private val colors = arrayOf(
        Color.CYAN, Color.YELLOW, Color.MAGENTA,
        Color.RED, Color.GREEN, Color.BLUE, Color.parseColor("#FFA500")
    )

    inner class Piece(val shape: Array<IntArray>, val color: Int, val elementId: String) {
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

            return Piece(newShape, color, elementId)
        }
    }

    fun setElementRequestListener(listener: () -> Unit) {
        elementRequestListener = {
            if (!isRequestingElement) {
                isRequestingElement = true
                listener()
            }
        }
    }

    fun setOnElementClickListener(listener: (String) -> Unit) {
        elementClickListener = listener
    }

    fun addNewElement(element: ElementModel) {
        activeElements.add(element)
        isRequestingElement = false
        if (!handler.hasCallbacks(updateRunnable) && !isPaused) {
            handler.post(updateRunnable)
        }
        if (currentPiece == null && !isPaused) {
            spawnPiece()
            invalidate()
        }
    }

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (!isPaused && currentPiece != null && !moveDown()) {
                placePiece()
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
        for (i in 0 until gridWidth) {
            for (j in 0 until gridHeight) {
                grid[i][j] = 0
                elementGrid[i][j] = null
            }
        }
        spawnPiece()
        handler.post(updateRunnable)
    }


    fun spawnPiece() {
        if (activeElements.isEmpty()) {
            elementRequestListener?.invoke()
            return
        }

        try {
            currentElement = activeElements.first()
            val shape = getPiece(currentElement!!.blockForm.toInt())
            currentPiece = Piece(shape, colors.random(), currentElement!!.id)
            currentPiece?.x = gridWidth / 2 - shape[0].size / 2
            currentPiece?.y = 0
            if (!handler.hasCallbacks(updateRunnable) && !isPaused) {
                handler.post(updateRunnable)
            }

            invalidate()
        } catch (e: Exception) {
            Log.e("TetrisView", "Error creating piece: ${e.message}")
            activeElements.remove(currentElement)
            currentElement = null
            if (activeElements.isNotEmpty()) {
                spawnPiece()
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
                        elementGrid[x][y] = piece.elementId
                    }
                }
            }
        }

        activeElements.remove(element)
        placedElements.add(element)
        currentPiece = null
        currentElement = null

        checkFilledLines() // Заменяем clearLines на checkFilledLines

        if (activeElements.isEmpty()) {
            elementRequestListener?.invoke()
        } else {
            spawnPiece()
            invalidate()
        }
    }

    private fun checkFilledLines() {
        for (j in 0 until gridHeight) {
            var isLineFull = true
            for (i in 0 until gridWidth) {
                if (grid[i][j] == 0) {
                    isLineFull = false
                    break
                }
            }

            if (isLineFull && filledLines?.contains(j) == false) {

                lineFilledListener?.invoke(j)
                filledLines?.add(j)
            }
        }
    }

    fun setLineFilledListener(listener: (Int) -> Unit) {
        lineFilledListener = listener
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

    fun setGridSize(width: Int, height: Int) {
        handler.removeCallbacks(updateRunnable)
        val newGrid = Array(width) { IntArray(height) }
        val newElementGrid = Array(width) { Array<String?>(height) { null } }
        val minWidth = minOf(gridWidth, width)
        val minHeight = minOf(gridHeight, height)

        for (i in 0 until minWidth) {
            for (j in 0 until minHeight) {
                newGrid[i][j] = grid[i][j]
                newElementGrid[i][j] = elementGrid[i][j]
            }
        }
        grid = newGrid
        elementGrid = newElementGrid
        gridWidth = width
        gridHeight = height
        currentPiece = null
        currentElement = null
        invalidate()
        if (!isPaused && activeElements.isNotEmpty()) {
            spawnPiece()
            handler.post(updateRunnable)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = (event.x / cellSize).toInt()
            val y = (event.y / cellSize).toInt()

            if (x in 0 until gridWidth && y in 0 until gridHeight) {
                elementGrid[x][y]?.let { elementId ->
                    elementClickListener?.invoke(elementId)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
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
        val desiredWidth = (gridWidth * cellSize).toInt()
        val desiredHeight = (gridHeight * cellSize).toInt()

        val width = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.AT_MOST -> minOf(desiredWidth, MeasureSpec.getSize(widthMeasureSpec))
            else -> desiredWidth
        }

        val height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(heightMeasureSpec)
            MeasureSpec.AT_MOST -> minOf(desiredHeight, MeasureSpec.getSize(heightMeasureSpec))
            else -> desiredHeight
        }

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