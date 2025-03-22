package com.example.bookmeter.fragments

import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.LeadingMarginSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.bookmeter.BuildConfig
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentBookDetailBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection
import kotlin.math.ceil

class BookDetailFragment : Fragment() {
    private var _binding: FragmentBookDetailBinding? = null
    private val binding get() = _binding!!
    private val args: BookDetailFragmentArgs by navArgs()
    private val apiKey = BuildConfig.GOOGLE_BOOKS_API_KEY
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBookDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayBookInfo()

        if (args.description.isEmpty() || args.description == "Fetching book details...") {
            fetchBookDetails(args.bookId)
        } else {
            // Set full description from args
            setFormattedDescription(args.description)
        }
    }

    private fun displayBookInfo() {
        binding.tvBookTitle.text = args.bookTitle
        binding.tvAuthor.text = args.author
        
        // We'll handle description separately with HTML formatting
        setFormattedDescription(args.description)

        if (args.imageUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(args.imageUrl)
                .placeholder(R.drawable.ic_book_placeholder)
                .error(R.drawable.ic_book_placeholder)
                .into(binding.bookCoverImage)
        } else {
            binding.bookCoverImage.setImageResource(R.drawable.ic_book_placeholder)
        }
    }

    private fun setFormattedDescription(description: String) {
        // Apply HTML formatting to make the description more readable
        if (description.isNotEmpty() && description != "Fetching book details...") {
            // Format HTML if it exists in the description
            val formattedText = if (description.contains("<") && description.contains(">")) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY)
                } else {
                    @Suppress("DEPRECATION")
                    Html.fromHtml(description)
                }
            } else {
                description
            }
            
            // Show estimated reading time
            val wordCount = description.split("\\s+".toRegex()).size
            val readingTimeMinutes = ceil(wordCount / 200.0).toInt() // Average reading speed
            binding.tvReadingTime.visibility = View.VISIBLE
            binding.tvReadingTime.text = "Estimated reading time: ${readingTimeMinutes} min"
            
            // Add paragraph indentation using spans if needed
            if (formattedText is SpannableString && !description.contains("<p>")) {
                val paragraphs = formattedText.toString().split("\n\n")
                if (paragraphs.size > 1) {
                    val spannable = SpannableString(formattedText)
                    var start = 0
                    for (paragraph in paragraphs) {
                        if (paragraph.isNotEmpty()) {
                            spannable.setSpan(
                                LeadingMarginSpan.Standard(48, 0),
                                start,
                                start + paragraph.length,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        start += paragraph.length + 2 // +2 for the "\n\n"
                    }
                    binding.tvDescription.text = spannable
                } else {
                    binding.tvDescription.text = formattedText
                }
            } else {
                binding.tvDescription.text = formattedText
            }
            
            binding.tvDescription.movementMethod = LinkMovementMethod.getInstance() // Enable links if present
        } else {
            binding.tvDescription.text = "No description available for this book."
            binding.tvReadingTime.visibility = View.GONE
        }
    }

    private fun fetchBookDetails(bookId: String) {
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val encodedBookId = URLEncoder.encode(bookId, "UTF-8")
                val requestUrl = "https://www.googleapis.com/books/v1/volumes/$encodedBookId?key=$apiKey"

                val url = URL(requestUrl)
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val jsonResponse = JSONObject(response)

                    val volumeInfo = jsonResponse.optJSONObject("volumeInfo")
                    if (volumeInfo != null) {
                        val title = volumeInfo.optString("title", args.bookTitle)

                        val authors = volumeInfo.optJSONArray("authors")?.let {
                            (0 until it.length()).joinToString(", ") { i -> it.getString(i) }
                        } ?: args.author

                        // Get the complete description - NO TRUNCATION
                        val description = when {
                            volumeInfo.has("description") -> volumeInfo.getString("description")
                            jsonResponse.optJSONObject("searchInfo")?.has("textSnippet") == true ->
                                jsonResponse.getJSONObject("searchInfo").getString("textSnippet")
                            else -> "No description available."
                        }

                        var imageUrl = args.imageUrl
                        volumeInfo.optJSONObject("imageLinks")?.let { imageLinks ->
                            val preferredKeys = listOf("extraLarge", "large", "medium", "small", "thumbnail", "smallThumbnail")
                            for (key in preferredKeys) {
                                if (imageLinks.has(key)) {
                                    imageUrl = imageLinks.getString(key)
                                    if (imageUrl.startsWith("http:")) {
                                        imageUrl = imageUrl.replace("http:", "https:")
                                    }
                                    break
                                }
                            }
                        }

                        withContext(Dispatchers.Main) {
                            binding.tvBookTitle.text = title
                            binding.tvAuthor.text = authors
                            
                            // Set the complete, untruncated description
                            setFormattedDescription(description)

                            if (imageUrl.isNotEmpty()) {
                                Glide.with(requireContext())
                                    .load(imageUrl)
                                    .placeholder(R.drawable.ic_book_placeholder)
                                    .error(R.drawable.ic_book_placeholder)
                                    .into(binding.bookCoverImage)
                            }

                            showLoading(false)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showLoading(false)
                            showError("Could not find book information")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showLoading(false)
                        showError("Error fetching book details: $responseCode")
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError("Network error: ${e.message}")
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
