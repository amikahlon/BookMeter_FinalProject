package com.example.bookmeter.model

data class Post(
    val id: String = "",  // Removed @DocumentId annotation
    val userId: String = "",
    val title: String = "",
    val review: String = "",
    val rating: Int = 0,
    val bookId: String = "",
    val bookName: String = "",
    val bookImageUrl: String = "",
    val imageUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0,
    val comments: Int = 0
)
