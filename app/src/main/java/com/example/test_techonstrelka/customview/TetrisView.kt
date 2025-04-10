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
    private val elementBlockForms = mutableMapOf<String, Int>()
    private val elementColors = mutableMapOf<String, Int>()
    private var gridWidth: Int = 10
    private var gridHeight: Int = 20
    private val paint = Paint()
    private val cellSize: Float
        get() = minOf(
            (width - paddingStart - paddingEnd).toFloat() / gridWidth,
            (height - paddingTop - paddingBottom).toFloat() / gridHeight
        )
    private var grid = Array(gridWidth) { IntArray(gridHeight) }
    private var elementGrid = Array(gridWidth) { Array<String?>(gridHeight) { null } }
    var currentPiece: Piece? = null
    private val handler = Handler(Looper.getMainLooper())
    private val updateDelay = 1000L
    var isPaused = false
    val activeElements = mutableListOf<ElementModel>()
    private val placedElements = mutableListOf<ElementModel>()
    private var currentElement: ElementModel? = null
    private var elementRequestListener: (() -> Unit)? = null
    private var elementClickListener: ((String) -> Unit)? = null
    private var lineFilledListener: ((Int) -> Unit)? = null
    private var filledLines: MutableList<Int>? = mutableListOf()
    private var isRequestingElement = false
    private var gameOverListener: (() -> Unit)? = null

    fun setGameOverListener(listener: () -> Unit) {
        gameOverListener = listener
    }
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
        handler.post { checkFilledLines() }
    }
    fun refreshElements(elements: List<ElementModel>) {
        activeElements.clear()
        activeElements.addAll(elements)
        placedElements.clear()
        for (i in 0 until gridWidth) {
            for (j in 0 until gridHeight) {
                grid[i][j] = 0
                elementGrid[i][j] = null
            }
        }
        invalidate()
        if (!handler.hasCallbacks(updateRunnable) && !isPaused) {
            handler.post(updateRunnable)
        }
    }
    private fun applyGravity() {
        val elementIds = elementGrid.flatten().filterNotNull().toSet().toList()

        val elementsWithMinY = elementIds.map { id ->
            val positions = mutableListOf<Pair<Int, Int>>()
            for (x in 0 until gridWidth) {
                for (y in 0 until gridHeight) {
                    if (elementGrid[x][y] == id) {
                        positions.add(Pair(x, y))
                    }
                }
            }
            val minY = positions.minByOrNull { it.second }?.second ?: 0
            id to minY
        }.sortedBy { it.second }

        for ((elementId, _) in elementsWithMinY) {
            val blockForm = elementBlockForms[elementId] ?: continue
            val shape = getPiece(blockForm)
            val color = elementColors[elementId] ?: continue

            val positions = mutableListOf<Pair<Int, Int>>()
            for (x in 0 until gridWidth) {
                for (y in 0 until gridHeight) {
                    if (elementGrid[x][y] == elementId) {
                        positions.add(Pair(x, y))
                    }
                }
            }
            if (positions.isEmpty()) continue

            val minX = positions.minByOrNull { it.first }?.first ?: 0
            val minYCurrent = positions.minByOrNull { it.second }?.second ?: 0

            positions.forEach { (x, y) ->
                grid[x][y] = 0
                elementGrid[x][y] = null
            }

            var newY = minYCurrent
            var maxPossibleY = minYCurrent
            while (newY < gridHeight) {
                if (isValidPositionForShape(shape, minX, newY)) {
                    maxPossibleY = newY
                    newY++
                } else {
                    break
                }
            }

            for (i in 0 until shape.size) {
                for (j in 0 until shape[i].size) {
                    if (shape[i][j] == 1) {
                        val xPos = minX + i
                        val yPos = maxPossibleY + j
                        if (xPos < gridWidth && yPos < gridHeight && grid[xPos][yPos] == 0) {
                            grid[xPos][yPos] = color
                            elementGrid[xPos][yPos] = elementId
                        }
                    }
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

            // После поворота нужно проверить, не выходит ли фигура за границы
            val rotatedPiece = Piece(newShape, color, elementId)
            rotatedPiece.x = this.x
            rotatedPiece.y = this.y

            // Корректировка позиции, если фигура выходит за границы
            if (rotatedPiece.x + rotatedPiece.shape.size > gridWidth) {
                rotatedPiece.x = gridWidth - rotatedPiece.shape.size
            }
            if (rotatedPiece.x < 0) {
                rotatedPiece.x = 0
            }

            return rotatedPiece
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
                    gameOverListener?.invoke() // Add this line
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
        elementBlockForms[element.id] = element.blockForm.toInt()
        elementColors[element.id] = piece.color

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
    private fun isValidPositionForShape(shape: Array<IntArray>, x: Int, y: Int): Boolean {
        for (i in 0 until shape.size) {
            for (j in 0 until shape[i].size) {
                if (shape[i][j] == 1) {
                    val newX = x + i
                    val newY = y + j
                    if (newX >= gridWidth || newY >= gridHeight) return false
                    if (newY >= 0 && grid[newX][newY] != 0) return false
                }
            }
        }
        return true
    }

    private fun rotatePiece(): Boolean {
        val piece = currentPiece ?: return false
        val rotated = piece.rotate()

        // Проверяем все возможные смещения, если поворот невозможен в текущей позиции
        val offsets = arrayOf(0, 1, -1, 2, -2) // Проверяем текущую позицию и смещения влево/вправо

        for (offset in offsets) {
            rotated.x = piece.x + offset
            if (isValidPosition(rotated.shape, rotated.x, rotated.y)) {
                currentPiece = rotated
                return true
            }
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
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        for (i in 0 until gridWidth) {
            for (j in 0 until gridHeight) {
                // Заливка клетки
                paint.color = if (grid[i][j] != 0) grid[i][j] else Color.parseColor("#333333")
                paint.style = Paint.Style.FILL
                canvas.drawRect(
                    i * cellSize,
                    j * cellSize,
                    (i + 1) * cellSize,
                    (j + 1) * cellSize,
                    paint
                )

                // Границы клетки
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
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val cellWidth = (widthSize - paddingStart - paddingEnd) / gridWidth
        val cellHeight = (heightSize - paddingTop - paddingBottom) / gridHeight
        val cellSize = minOf(cellWidth, cellHeight)

        val desiredWidth = (cellSize * gridWidth + paddingStart + paddingEnd).toInt()
        val desiredHeight = (cellSize * gridHeight + paddingTop + paddingBottom).toInt()

        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
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
        if (rotatePiece()) {
            invalidate()
        }
    }

    fun onDrop() {
        while (moveDown()) {

        }
        invalidate()
    }
}