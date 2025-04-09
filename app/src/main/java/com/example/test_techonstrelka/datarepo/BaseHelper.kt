package com.example.test_techonstrelka.datarepo;


import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log;

class SimpleDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "Tasks.db"
        private const val DATABASE_VERSION = 3

        private const val TABLE_TASKS = "tasks"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name_task"
        private const val COLUMN_DESC = "description_task"
        private const val COLUMN_LEVEL = "level" // LEVEL = 0 -> ВЫПОЛНЕНА ЗАДАЧА, LEVEL = 1 -> ОБЫЧНАЯ ЗАДАЧА, LEVEL = 2 -> POMODORO ЗАДАЧА
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_TIME = "time" //КОЛИЧЕСТВО 30 МИНУТ В ПЕРИОДЕ ВРЕМЕНИ(2 часа = 4)
        private const val COLUMN_BLOCKFORM = "blockform" // форма блока
        private const val COLUMN_INTERVAL = "interval" // 0-> day 1-> week 2-> month

        private const val CREATE_TABLE_TASKS = """
            CREATE TABLE $TABLE_TASKS (
                $COLUMN_ID TEXT PRIMARY KEY,
                $COLUMN_NAME TEXT,
                $COLUMN_DESC TEXT,
                $COLUMN_LEVEL INTEGER,
                $COLUMN_CATEGORY TEXT,
                $COLUMN_TIME TEXT, 
                $COLUMN_BLOCKFORM INTEGER,
                $COLUMN_INTERVAL INTEGER
            )
        """
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_TASKS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
        onCreate(db)
    }
}
