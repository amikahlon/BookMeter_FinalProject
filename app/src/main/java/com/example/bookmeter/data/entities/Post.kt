package com.example.bookmeter.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.bookmeter.data.converters.StringListConverter
import com.example.bookmeter.model.Post

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey
    val id: String,
    val userId: String,
    val title: String,
    val review: String,
    val rating: Int,
    val bookId: String,
    val bookName: String,
    val bookImageUrl: String,
    val imageUrl: String,
    val timestamp: Long,
    val likes: Int,
    val comments: Int,
    
    @TypeConverters(StringListConverter::class)
    val likedBy: List<String>
) {
    // Default constructor for Room
    constructor() : this(
        id = "",
        userId = "",
        title = "",
        review = "",
        rating = 0,
        bookId = "",
        bookName = "",
        bookImageUrl = "",
        imageUrl = "",
        timestamp = 0L,
        likes = 0,
        comments = 0,
        likedBy = emptyList()
    )

    // Conversion function from model to entity
    companion object {
        fun fromModel(model: com.example.bookmeter.model.Post): Post {
            return Post(
                id = model.id,
                userId = model.userId,
                title = model.title,
                review = model.review,
                rating = model.rating,
                bookId = model.bookId,
                bookName = model.bookName,
                bookImageUrl = model.bookImageUrl,
                imageUrl = model.imageUrl,
                timestamp = model.timestamp,
                likes = model.likes,
                comments = model.comments,
                likedBy = model.likedBy
            )
        }
    }

    // Conversion function to model
    fun toModel(): com.example.bookmeter.model.Post {
        return com.example.bookmeter.model.Post(
            id = id,
            userId = userId,
            title = title,
            review = review,
            rating = rating,
            bookId = bookId,
            bookName = bookName,
            bookImageUrl = bookImageUrl,
            imageUrl = imageUrl,
            timestamp = timestamp,
            likes = likes,
            comments = comments,
            likedBy = likedBy
        )
    }
}
