package com.example.bookmeter.model

data class Post(
    val id: String = "",
    val bookId: String = "",      // Added bookId field
    val bookName: String = "",
    val bookImageUrl: String = "",
    val title: String = "",
    val review: String = "",
    val rating: Int = 0,
    val userId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
