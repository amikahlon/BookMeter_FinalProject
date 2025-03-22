package com.example.bookmeter.ui.adapters

import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookmeter.BuildConfig
import com.example.bookmeter.R
import com.example.bookmeter.databinding.ItemWishlistBinding
import com.example.bookmeter.fragments.WishListFragmentDirections
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

class WishlistAdapter : RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder>() {

    private var wishlistBooks: List<String> = emptyList()
    private val bookDetailsCache = mutableMapOf<String, BookDetails>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val db = FirebaseFirestore.getInstance()
    private val apiKey = BuildConfig.GOOGLE_BOOKS_API_KEY
    private val TAG = "WishlistAdapter"
    
    // Add a listener interface for book removals
    interface OnBookRemovedListener {
        fun onBookRemoved(bookId: String)
    }
    
    private var bookRemovedListener: OnBookRemovedListener? = null
    
    // Method to set the listener
    fun setOnBookRemovedListener(listener: OnBookRemovedListener) {
        this.bookRemovedListener = listener
    }

    fun submitList(newWishlist: List<String>) {
        wishlistBooks = newWishlist
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val binding = ItemWishlistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WishlistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        val bookId = wishlistBooks[position]
        if (bookDetailsCache.containsKey(bookId)) {
            holder.bind(bookId, bookDetailsCache[bookId])
        } else {
            holder.bind(bookId, null)
            fetchBookDetails(bookId) { bookDetails ->
                bookDetailsCache[bookId] = bookDetails
                if (holder.adapterPosition == position && holder.adapterPosition != RecyclerView.NO_POSITION) {
                    holder.bind(bookId, bookDetails)
                }
            }
        }
    }

    override fun getItemCount(): Int = wishlistBooks.size

    private fun fetchBookDetails(bookId: String, callback: (BookDetails) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val encodedBookId = URLEncoder.encode(bookId, "UTF-8")
                val success = tryGetBookByVolumeId(encodedBookId, callback)
                if (!success) {
                    trySearchForBook(bookId, callback)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(createErrorBookDetails("Error", e.message ?: "Unknown error"))
                }
            }
        }
    }

    private suspend fun tryGetBookByVolumeId(encodedBookId: String, callback: (BookDetails) -> Unit): Boolean {
        return try {
            val requestUrl = "https://www.googleapis.com/books/v1/volumes/$encodedBookId?key=$apiKey"
            val url = URL(requestUrl)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                processBookResponse(jsonResponse, callback)
                connection.disconnect()
                true
            } else {
                connection.disconnect()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in tryGetBookByVolumeId: ${e.message}", e)
            false
        }
    }

    private suspend fun trySearchForBook(bookId: String, callback: (BookDetails) -> Unit): Boolean {
        return try {
            val encodedQuery = URLEncoder.encode("\"$bookId\"", "UTF-8")
            val requestUrl = "https://www.googleapis.com/books/v1/volumes?q=$encodedQuery&key=$apiKey"
            val url = URL(requestUrl)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                val jsonResponse = JSONObject(response)
                if (jsonResponse.has("items")) {
                    val firstBook = jsonResponse.getJSONArray("items").getJSONObject(0)
                    processBookResponse(firstBook, callback)
                    connection.disconnect()
                    true
                } else {
                    connection.disconnect()
                    false
                }
            } else {
                connection.disconnect()
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in trySearchForBook: ${e.message}", e)
            false
        }
    }

    private suspend fun processBookResponse(jsonResponse: JSONObject, callback: (BookDetails) -> Unit) {
        try {
            val volumeInfo = jsonResponse.optJSONObject("volumeInfo") ?: run {
                withContext(Dispatchers.Main) {
                    callback(createErrorBookDetails("No volume info", ""))
                }
                return
            }

            val title = volumeInfo.optString("title", "Unknown Title")
            val authors = volumeInfo.optJSONArray("authors")?.let {
                (0 until it.length()).joinToString(", ") { i -> it.getString(i) }
            } ?: "Unknown Author"

            val rawDescription = volumeInfo.optString("description", "")
            val formattedDescription = if (rawDescription.isNotEmpty()) formatDescription(rawDescription)
            else volumeInfo.optString("subtitle", "No description available.")

            var imageUrl = ""
            volumeInfo.optJSONObject("imageLinks")?.let { imageLinks ->
                val preferredKeys = listOf("thumbnail", "smallThumbnail", "medium", "large")
                for (key in preferredKeys) {
                    if (imageLinks.has(key)) {
                        imageUrl = imageLinks.getString(key)
                        break
                    }
                }
                if (imageUrl.startsWith("http:")) {
                    imageUrl = imageUrl.replace("http:", "https:")
                }
            }

            val bookDetails = BookDetails(title, authors, formattedDescription, imageUrl)
            withContext(Dispatchers.Main) {
                callback(bookDetails)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing book response: ${e.message}", e)
            withContext(Dispatchers.Main) {
                callback(createErrorBookDetails("Error", "${e.message}"))
            }
        }
    }

    private fun formatDescription(raw: String): String {
        try {
            val clean = fromHtml(raw).toString().trim()
            if (clean.isBlank()) return "No description available."
            
            // Find the first sentence (ending with a period, exclamation, or question mark)
            val sentenceEnd = clean.indexOf('.').let { if (it >= 0) it else clean.length }
                .coerceAtMost(clean.indexOf('!').let { if (it >= 0) it else clean.length })
                .coerceAtMost(clean.indexOf('?').let { if (it >= 0) it else clean.length })
            
            // If we found a sentence end, truncate there, otherwise take the first 100 chars
            val truncated = when {
                sentenceEnd > 0 && sentenceEnd < clean.length - 1 -> 
                    clean.substring(0, sentenceEnd + 1) + " ..."
                clean.length > 100 -> clean.substring(0, 100) + " ..."
                else -> clean
            }
            
            return truncated
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting description: ${e.message}")
            val truncated = if (raw.length > 100) raw.substring(0, 100) + "..." else raw
            return truncated.trim()
        }
    }

    private fun fromHtml(html: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }

    private fun createErrorBookDetails(title: String, message: String): BookDetails {
        return BookDetails(
            "$title - $message",
            "",
            "We couldn't load this book's details. Try again later or remove it from your wishlist.",
            ""
        )
    }

    inner class WishlistViewHolder(private val binding: ItemWishlistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnRemoveFromWishlist.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION && currentUserId != null) {
                    val bookId = wishlistBooks[position]
                    removeFromWishlist(bookId)
                }
            }
            
            binding.btnViewDetails.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val bookId = wishlistBooks[position]
                    val bookDetails = bookDetailsCache[bookId]
                    
                    // Navigate to book detail fragment with book info - FIXED: force full description fetch
                    val action = WishListFragmentDirections.actionWishListFragmentToBookDetailFragment(
                        bookId,
                        bookDetails?.title ?: "Loading...",
                        bookDetails?.author ?: "",
                        "", // Pass empty string to force fetching the full description
                        bookDetails?.imageUrl ?: ""
                    )
                    binding.root.findNavController().navigate(action)
                }
            }
        }

        fun bind(bookId: String, bookDetails: BookDetails?) {
            if (bookDetails == null) {
                binding.tvBookTitle.text = "Loading..."
                binding.tvAuthor.text = "Please wait"
                binding.tvDescription.text = "Fetching book details..."
                binding.bookCoverImage.setImageResource(R.drawable.ic_book_placeholder)
                binding.btnViewDetails.isEnabled = false
            } else {
                binding.tvBookTitle.text = bookDetails.title
                binding.tvAuthor.text = bookDetails.author
                binding.tvDescription.text = formatDescription(bookDetails.description)
                binding.btnViewDetails.isEnabled = true
                if (bookDetails.imageUrl.isNotEmpty()) {
                    Glide.with(binding.root.context)
                        .load(bookDetails.imageUrl)
                        .placeholder(R.drawable.ic_book_placeholder)
                        .error(R.drawable.ic_book_placeholder)
                        .into(binding.bookCoverImage)
                } else {
                    binding.bookCoverImage.setImageResource(R.drawable.ic_book_placeholder)
                }
            }
        }

        private fun removeFromWishlist(bookId: String) {
            currentUserId?.let { uid ->
                val userRef = db.collection("users").document(uid)
                userRef.update("wishlistBooks", FieldValue.arrayRemove(bookId))
                    .addOnSuccessListener {
                        Snackbar.make(binding.root, "Book removed from wishlist", Snackbar.LENGTH_SHORT).show()
                        // Notify listener that book was removed
                        bookRemovedListener?.onBookRemoved(bookId)
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(binding.root, "Failed to remove book: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
            }
        }
    }

    data class BookDetails(
        val title: String,
        val author: String,
        val description: String,
        val imageUrl: String
    )
}
