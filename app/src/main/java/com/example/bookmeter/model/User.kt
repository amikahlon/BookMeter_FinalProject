package com.example.bookmeter.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profilePictureUrl: String = "",
    val wishlistBooks: List<String> = emptyList(),
    val readBooks: List<String> = listOf(),
    val postIds: List<String> = listOf()
)