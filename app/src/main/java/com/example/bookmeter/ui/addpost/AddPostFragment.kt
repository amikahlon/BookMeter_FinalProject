package com.example.bookmeter.ui.addpost

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentAddPostBinding
import com.example.bookmeter.model.Book
import com.example.bookmeter.repository.BookRepository
import com.example.bookmeter.ui.adapters.BookAdapter
import com.example.bookmeter.ui.adapters.FormAdapter
import com.example.bookmeter.utils.PermissionHelper
import com.example.bookmeter.viewmodels.BookViewModel
import com.example.bookmeter.viewmodels.BookViewModelFactory
import com.example.bookmeter.viewmodels.PostViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.math.roundToInt

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var postViewModel: PostViewModel
    private lateinit var bookViewModel: BookViewModel
    private lateinit var bookAdapter: BookAdapter
    private lateinit var formAdapter: FormAdapter
    
    private var selectedBook: Book? = null
    private var postImageUri: Uri? = null // For storing selected image URI
    
    // View references from adapter items
    private var bookNameEditText: TextInputEditText? = null
    private var bookNameLayout: TextInputLayout? = null
    private var bookSearchProgress: View? = null
    private var bookSearchResults: androidx.recyclerview.widget.RecyclerView? = null
    private var selectedBookCard: View? = null
    private var selectedBookTitle: android.widget.TextView? = null
    private var selectedBookAuthor: android.widget.TextView? = null
    private var selectedBookImage: android.widget.ImageView? = null
    private var btnClearSelectedBook: android.widget.ImageButton? = null
    private var reviewTitleEditText: TextInputEditText? = null
    private var titleLayout: TextInputLayout? = null
    private var reviewEditText: TextInputEditText? = null
    private var reviewLayout: TextInputLayout? = null
    private var ratingBar: RatingBar? = null
    private var ratingText: android.widget.TextView? = null
    
    // Image upload view references
    private var imageContainer: View? = null
    private var postImageView: android.widget.ImageView? = null
    private var textNoImage: android.widget.TextView? = null
    private var btnSelectImage: View? = null
    private var btnRemoveImage: View? = null
    
    // Register permission and activity launchers
    private var hasShownRationale = false
    
    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        when {
            isGranted -> {
                openImagePicker()
            }
            shouldShowRequestPermissionRationale(PermissionHelper.READ_EXTERNAL_STORAGE_PERMISSION) -> {
                if (!hasShownRationale) {
                    hasShownRationale = true
                } else {
                    Toast.makeText(context, "Permission is needed to select an image", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
            }
        }
    }
    
    // Image picker launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                setPostImage(uri)
            }
        }
    }
    
    // Settings launcher
    private val openSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check permission again after returning from settings
        if (PermissionHelper.hasStoragePermission(requireContext())) {
            openImagePicker()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddPostBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        postViewModel = ViewModelProvider(this)[PostViewModel::class.java]
        
        // Initialize BookViewModel with its factory
        val bookRepository = BookRepository()
        bookViewModel = ViewModelProvider(this, BookViewModelFactory(bookRepository))[BookViewModel::class.java]
        
        setupFormRecyclerView()
        setupObservers()
        setupListeners()
    }
    
    private fun setupFormRecyclerView() {
        formAdapter = FormAdapter()
        
        binding.formRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = formAdapter
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        }
        
        // Access views after they are created in the RecyclerView
        binding.formRecyclerView.post {
            fetchBookSearchViewReferences()
            fetchReviewDetailsViewReferences()
            fetchRatingViewReferences()
            fetchImageUploadViewReferences() // New method
            setupBookSearch()
            setupInitialViewState()
        }
    }
    
    private fun fetchBookSearchViewReferences() {
        val bookSearchViewHolder = binding.formRecyclerView
            .findViewHolderForAdapterPosition(FormAdapter.TYPE_BOOK_SEARCH) as? FormAdapter.BookSearchViewHolder
        
        bookSearchViewHolder?.let {
            bookNameEditText = it.binding.editBookName
            bookNameLayout = it.binding.bookNameLayout
            bookSearchProgress = it.binding.bookSearchProgress
            bookSearchResults = it.binding.bookSearchResults
            selectedBookCard = it.binding.selectedBookCard
            selectedBookTitle = it.binding.selectedBookTitle
            selectedBookAuthor = it.binding.selectedBookAuthor
            selectedBookImage = it.binding.selectedBookImage
            btnClearSelectedBook = it.binding.btnClearSelectedBook
            
            bookNameEditText?.doAfterTextChanged { text ->
                val query = text.toString().trim()
                if (query.length >= 3) {
                    bookSearchProgress?.visibility = View.VISIBLE
                    bookViewModel.searchBooks(query)
                } else {
                    bookSearchResults?.visibility = View.GONE
                }
            }
            
            btnClearSelectedBook?.setOnClickListener {
                clearSelectedBook()
            }
        }
    }
    
    private fun fetchReviewDetailsViewReferences() {
        val reviewViewHolder = binding.formRecyclerView
            .findViewHolderForAdapterPosition(FormAdapter.TYPE_REVIEW_DETAILS) as? FormAdapter.ReviewDetailsViewHolder
            
        reviewViewHolder?.let {
            reviewTitleEditText = it.binding.editTitle
            titleLayout = it.binding.titleLayout
            reviewEditText = it.binding.editReview
            reviewLayout = it.binding.reviewLayout
        }
    }
    
    private fun fetchRatingViewReferences() {
        val ratingViewHolder = binding.formRecyclerView
            .findViewHolderForAdapterPosition(FormAdapter.TYPE_RATING) as? FormAdapter.RatingViewHolder
            
        ratingViewHolder?.let {
            ratingBar = it.binding.ratingBar
            ratingText = it.binding.ratingText
            
            ratingBar?.setOnRatingBarChangeListener { _, rating, fromUser ->
                if (fromUser) {
                    animateRatingChange(rating)
                }
            }
        }
    }
    
    private fun fetchImageUploadViewReferences() {
        val imageUploadViewHolder = binding.formRecyclerView
            .findViewHolderForAdapterPosition(FormAdapter.TYPE_IMAGE_UPLOAD) as? FormAdapter.ImageUploadViewHolder
            
        imageUploadViewHolder?.let {
            imageContainer = it.binding.imageContainer
            postImageView = it.binding.postImageView
            textNoImage = it.binding.textNoImage
            btnSelectImage = it.binding.btnSelectImage
            btnRemoveImage = it.binding.btnRemoveImage
            
            btnSelectImage?.setOnClickListener {
                checkPermissionAndOpenImagePicker()
            }
            
            btnRemoveImage?.setOnClickListener {
                removePostImage()
            }
        }
    }
    
    private fun checkPermissionAndOpenImagePicker() {
        if (PermissionHelper.hasStoragePermission(requireContext())) {
            openImagePicker()
        } else {
            requestPermissionLauncher.launch(PermissionHelper.READ_EXTERNAL_STORAGE_PERMISSION)
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }
    
    private fun setPostImage(uri: Uri) {
        postImageUri = uri
        
        // Show the image
        textNoImage?.visibility = View.GONE
        postImageView?.visibility = View.VISIBLE
        btnRemoveImage?.visibility = View.VISIBLE
        
        // Load image with Glide
        postImageView?.let {
            Glide.with(requireContext())
                .load(uri)
                .centerCrop()
                .into(it)
        }
        
        // Apply subtle animation to show the image was loaded
        postImageView?.alpha = 0f
        postImageView?.animate()
            ?.alpha(1f)
            ?.setDuration(300)
            ?.start()
    }
    
    private fun removePostImage() {
        postImageUri = null
        
        // Hide image view and show placeholder text
        postImageView?.visibility = View.GONE
        textNoImage?.visibility = View.VISIBLE
        btnRemoveImage?.visibility = View.GONE
        
        // Visual feedback when removing
        imageContainer?.let {
            it.alpha = 0.5f
            it.animate()
                .alpha(1f)
                .setDuration(300)
                .start()
        }
    }
    
    private fun animateRatingChange(rating: Float) {
        ratingBar?.animate()?.scaleX(1.2f)?.scaleY(1.2f)?.setDuration(200)?.withEndAction {
            ratingBar?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(200)
        }
        
        // Update the rating text
        val ratingTextValue = when (rating.roundToInt()) {
            1 -> "Poor"
            2 -> "Fair"
            3 -> "Good"
            4 -> "Very Good"
            5 -> "Excellent"
            else -> ""
        }
        ratingText?.text = ratingTextValue
        ratingText?.visibility = View.VISIBLE
        
        // Add color to rating text based on rating
        ratingText?.setTextColor(
            resources.getColor(
                when (rating.roundToInt()) {
                    1 -> android.R.color.holo_red_dark
                    2 -> android.R.color.holo_orange_dark
                    3 -> android.R.color.holo_orange_light
                    4 -> android.R.color.holo_green_light
                    5 -> android.R.color.holo_green_dark
                    else -> android.R.color.black
                }, null
            )
        )
    }
    
    private fun setupBookSearch() {
        // Set up the BookAdapter for search results
        bookAdapter = BookAdapter { book -> 
            selectBook(book)
        }
        
        bookSearchResults?.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = bookAdapter
        }
    }
    
    private fun setupInitialViewState() {
        selectedBookCard?.visibility = View.GONE
        bookSearchResults?.visibility = View.GONE
        ratingText?.visibility = View.GONE
        bookSearchProgress?.visibility = View.GONE
    }
    
    private fun selectBook(book: Book) {
        selectedBook = book
        
        // Update the book name field
        bookNameEditText?.setText(book.title)
        
        // Show selected book details in the card
        selectedBookCard?.visibility = View.VISIBLE
        selectedBookTitle?.text = book.title
        selectedBookAuthor?.text = book.authors.joinToString(", ")
        
        // Load book image with animation
        if (book.thumbnail.isNotEmpty()) {
            selectedBookImage?.alpha = 0f
            com.bumptech.glide.Glide.with(requireContext())
                .load(book.thumbnail)
                .into(selectedBookImage!!)
            
            selectedBookImage?.animate()
                ?.alpha(1f)
                ?.setDuration(300)
                ?.start()
        }
        
        // Hide search results after selection
        bookSearchResults?.animate()
            ?.alpha(0f)
            ?.setDuration(300)
            ?.withEndAction {
                bookSearchResults?.visibility = View.GONE
                bookSearchResults?.alpha = 1f
            }
            ?.start()
        
        // Animate the selection card
        (selectedBookCard as? MaterialCardView)?.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start()
        }
        
        // Provide haptic feedback
        selectedBookCard?.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    }
    
    private fun clearSelectedBook() {
        selectedBookCard?.animate()
            ?.alpha(0f)
            ?.scaleX(0.8f)
            ?.scaleY(0.8f)
            ?.setDuration(300)
            ?.withEndAction {
                selectedBook = null
                selectedBookCard?.visibility = View.GONE
                bookNameEditText?.text?.clear()
            }
            ?.start()
    }
    
    private fun setupObservers() {
        // Observe loading state
        postViewModel.isLoading.observe(viewLifecycleOwner) { isLoading -> 
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSubmitPost.isEnabled = !isLoading
        }
        
        // Observe post creation result
        postViewModel.newPostResult.observe(viewLifecycleOwner) { result -> 
            result?.let {
                if (it.isSuccess) {
                    showSuccessAndNavigate()
                } else {
                    val errorMessage = it.exceptionOrNull()?.message ?: "Failed to post review"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
                // Reset the result to avoid re-processing on config changes
                postViewModel.resetNewPostResult()
            }
        }
        
        // Observe book search results
        bookViewModel.books.observe(viewLifecycleOwner) { books -> 
            bookSearchProgress?.visibility = View.GONE
            bookAdapter.submitList(books)
            
            if (books.isNotEmpty()) {
                if (bookSearchResults?.visibility == View.GONE) {
                    bookSearchResults?.alpha = 0f
                    bookSearchResults?.visibility = View.VISIBLE
                    bookSearchResults?.animate()
                        ?.alpha(1f)
                        ?.translationY(0f)
                        ?.setDuration(300)
                        ?.start()
                }
            } else {
                bookSearchResults?.visibility = View.GONE
            }
        }
    }
    
    private fun showSuccessAndNavigate() {
        Toast.makeText(context, "Review posted successfully!", Toast.LENGTH_SHORT).show()
        // Navigate back to dashboard using the action we defined
        findNavController().navigate(R.id.action_addPostFragment_to_dashboardFragment)
    }
    
    private fun setupListeners() {
        binding.btnSubmitPost.setOnClickListener {
            submitPost()
        }
    }
    
    private fun submitPost() {
        val bookName = bookNameEditText?.text?.toString()?.trim() ?: ""
        val title = reviewTitleEditText?.text?.toString()?.trim() ?: ""
        val review = reviewEditText?.text?.toString()?.trim() ?: ""
        val rating = ratingBar?.rating?.roundToInt() ?: 0
        
        // Validate input
        if (!validateInput(bookName, title, review, rating)) {
            return
        }
        
        // Get book details from selected book or use entered text
        val bookId = selectedBook?.id ?: "placeholder_${System.currentTimeMillis()}"
        val bookImageUrl = selectedBook?.thumbnail ?: ""
        
        // Submit the post with animation feedback
        binding.btnSubmitPost.isEnabled = false
        binding.btnSubmitPost.animate()
            .scaleX(0.95f).scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                binding.btnSubmitPost.animate()
                    .scaleX(1f).scaleY(1f)
                    .setDuration(100)
                
                // Submit the post with the image URI
                postViewModel.createPost(
                    bookId = bookId,
                    bookName = bookName,
                    bookImageUrl = bookImageUrl,
                    title = title,
                    review = review,
                    rating = rating,
                    imageUri = postImageUri
                )
            }
    }
    
    private fun validateInput(bookName: String, title: String, review: String, rating: Int): Boolean {
        var isValid = true
        
        if (bookName.isEmpty()) {
            bookNameLayout?.error = "Book name is required"
            isValid = false
        } else {
            bookNameLayout?.error = null
        }
        
        if (title.isEmpty()) {
            titleLayout?.error = "Title is required"
            isValid = false
        } else {
            titleLayout?.error = null
        }
        
        if (review.isEmpty()) {
            reviewLayout?.error = "Review is required"
            isValid = false
        } else {
            reviewLayout?.error = null
        }
        
        if (rating == 0) {
            // Animate the rating bar to draw attention
            ratingBar?.animate()
                ?.scaleX(1.2f)?.scaleY(1.2f)
                ?.setDuration(200)
                ?.withEndAction {
                    ratingBar?.animate()
                        ?.scaleX(1f)?.scaleY(1f)
                        ?.setDuration(200)
                }
            Toast.makeText(context, "Please provide a rating", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        return isValid
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
