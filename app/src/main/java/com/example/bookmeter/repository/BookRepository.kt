package com.example.bookmeter.repository

import com.example.bookmeter.BuildConfig
import com.example.bookmeter.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class BookRepository {

    private val apiKey = BuildConfig.GOOGLE_BOOKS_API_KEY  // שליפת ה-API Key מ-BuildConfig

    suspend fun searchBooks(query: String): List<Book> {
        return withContext(Dispatchers.IO) {
            val url = "https://www.googleapis.com/books/v1/volumes?q=${query}&key=${apiKey}"
            val result = fetchFromApi(url)
            parseBookList(result)
        }
    }

    suspend fun getBookById(bookId: String): Book? {
        return withContext(Dispatchers.IO) {
            val url = "https://www.googleapis.com/books/v1/volumes/${bookId}?key=${apiKey}"
            val result = fetchFromApi(url)
            parseBook(result)
        }
    }

    private fun fetchFromApi(apiUrl: String): String {
        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseBookList(jsonString: String): List<Book> {
        val jsonObject = JSONObject(jsonString)
        val items = jsonObject.optJSONArray("items") ?: return emptyList()
        val books = mutableListOf<Book>()

        for (i in 0 until items.length()) {
            val bookJson = items.getJSONObject(i)
            parseBook(bookJson)?.let { books.add(it) }
        }

        return books
    }

    private fun parseBook(jsonString: String): Book? {
        val jsonObject = JSONObject(jsonString)
        return parseBook(jsonObject)
    }

    private fun parseBook(bookJson: JSONObject): Book? {
        val volumeInfo = bookJson.optJSONObject("volumeInfo") ?: return null
        val imageLinks = volumeInfo.optJSONObject("imageLinks")
        var thumbnailUrl = imageLinks?.optString("thumbnail", "") ?: ""

        // המרת HTTP ל-HTTPS במקרה שה-API מחזיר כתובת HTTP
        if (thumbnailUrl.startsWith("http://")) {
            thumbnailUrl = thumbnailUrl.replace("http://", "https://")
        }

        return Book(
            id = bookJson.optString("id"),
            title = volumeInfo.optString("title"),
            authors = volumeInfo.optJSONArray("authors")?.let {
                List(it.length()) { index -> it.getString(index) }
            } ?: emptyList(),
            description = volumeInfo.optString("description", "No description available"),
            thumbnail = thumbnailUrl
        )
    }
}
