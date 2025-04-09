package com.example.test_techonstrelka.datarepo

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.example.test_techonstrelka.models.Task


class TaskRepository(context: Context) {
    companion object {
        private const val DATABASE_NAME = "Tasks.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_TASKS = "tasks"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name_task"
        private const val COLUMN_DESC = "description_task"
        private const val COLUMN_LEVEL = "level"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_TIME = "time"
        private const val COLUMN_BLOCKFORM = "blockform"

        private const val CREATE_TABLE_TASKS = """
            CREATE TABLE $TABLE_TASKS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT,
                $COLUMN_DESC TEXT,
                $COLUMN_LEVEL INTEGER,
                $COLUMN_CATEGORY TEXT,
                $COLUMN_TIME TEXT,
                $COLUMN_BLOCKFORM INTEGER
            )
        """
    }
    private val databaseHelper = SimpleDatabaseHelper(context)

    // Добавление новой задачи
    fun addTask(
        id: String,
        name: String,
        description: String,
        level: Int,
        category: String,
        time: String,
        blockForm: Int
    ): Long {
        val db = databaseHelper.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_ID, id)
            put(COLUMN_NAME, name)
            put(COLUMN_DESC, description)
            put(COLUMN_LEVEL, level)
            put(COLUMN_CATEGORY, category)
            put(COLUMN_TIME, time)
            put(COLUMN_BLOCKFORM, blockForm)
        }
        return db.insert(TABLE_TASKS, null, values).also {
            db.close()
            if (it == -1L) Log.e("DB", "Ошибка добавления задачи")
        }
    }

    // Получение всех задач
    fun getAllTasks(): List<Task> {
        val tasks = mutableListOf<Task>()
        val db = databaseHelper.readableDatabase
        val cursor = db.query(
            TABLE_TASKS,
            null, null, null, null, null, null
        )

        while (cursor.moveToNext()) {
            tasks.add(Task(
                id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESC)),
                level = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LEVEL)),
                category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME)),
                blockForm = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BLOCKFORM))
            ))
        }
        cursor.close()
        db.close()
        return tasks
    }

    // Получение задачи по ID
    fun getTaskById(taskId: String?): Task? {
        val db = databaseHelper.readableDatabase
        val cursor = db.query(
            TABLE_TASKS,
            null,                         // Все столбцы
            "$COLUMN_ID = ?",             // Условие WHERE
            arrayOf(taskId.toString()),   // Аргументы для WHERE
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            Task(
                id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                description = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESC)),
                level = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LEVEL)),
                category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                time = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME)),
                blockForm = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BLOCKFORM))
            ).also {
                cursor.close()
                db.close()
            }
        } else {
            cursor.close()
            db.close()
            null
        }
    }

    fun deleteTask(id: String) {
        val db = databaseHelper.writableDatabase
        db.delete("tasks", "id = ?", arrayOf(id))
    }
}