package com.example.bookmeter.model

data class Post(
    val id: String = "",
    val userId: String = "",
    val bookId: String = "",
    val bookName: String = "",
    val bookImageUrl: String = "",
    val title: String = "",
    val review: String = "",
    val rating: Int = 0,
    val imageUrl: String = "", // Added field for post image
    val timestamp: Long = 0
)
