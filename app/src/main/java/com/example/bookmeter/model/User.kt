package com.example.bookmeter.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val profilePictureUrl: String = "",
    val favoriteGenres: List<String> = listOf(),
    val wishlistBooks: List<String> = listOf(),
    val readBooks: List<String> = listOf()
)