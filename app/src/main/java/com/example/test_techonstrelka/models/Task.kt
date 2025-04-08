package com.example.test_techonstrelka.models

data class Task(
    val id: String,
    val name: String,
    val description: String,
    val level: Int,
    val category: String,
    val time: String,
    val blockForm: Int
)
