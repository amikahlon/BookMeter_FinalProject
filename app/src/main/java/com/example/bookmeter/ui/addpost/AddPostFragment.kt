package com.example.bookmeter.ui.addpost

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentAddPostBinding
import com.example.bookmeter.viewmodels.PostViewModel
import kotlin.math.roundToInt

class AddPostFragment : Fragment() {

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var postViewModel: PostViewModel
    
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
        
        setupObservers()
        setupListeners()
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
                    Toast.makeText(context, "Review posted successfully!", Toast.LENGTH_SHORT).show()
                    // Navigate back to dashboard using the action we defined
                    findNavController().navigate(R.id.action_addPostFragment_to_dashboardFragment)
                } else {
                    val errorMessage = it.exceptionOrNull()?.message ?: "Failed to post review"
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
                // Reset the result to avoid re-processing on config changes
                postViewModel.resetNewPostResult()
            }
        }
    }
    
    private fun setupListeners() {
        binding.btnSubmitPost.setOnClickListener {
            submitPost()
        }
    }
    
    private fun submitPost() {
        val bookName = binding.editBookName.text?.toString()?.trim() ?: ""
        val title = binding.editTitle.text?.toString()?.trim() ?: ""
        val review = binding.editReview.text?.toString()?.trim() ?: ""
        val rating = binding.ratingBar.rating.roundToInt()
        
        // Validate input
        if (bookName.isEmpty()) {
            binding.bookNameLayout.error = "Book name is required"
            return
        } else {
            binding.bookNameLayout.error = null
        }
        
        if (title.isEmpty()) {
            binding.titleLayout.error = "Title is required"
            return
        } else {
            binding.titleLayout.error = null
        }
        
        if (review.isEmpty()) {
            binding.reviewLayout.error = "Review is required"
            return
        } else {
            binding.reviewLayout.error = null
        }
        
        if (rating == 0) {
            Toast.makeText(context, "Please provide a rating", Toast.LENGTH_SHORT).show()
            return
        }
        
        // For now, we'll use a placeholder for bookId
        val tempBookId = "placeholder_${System.currentTimeMillis()}"
        val bookImageUrl = "" // No image for now
        
        // Submit the post
        postViewModel.createPost(
            bookId = tempBookId,
            bookName = bookName,
            bookImageUrl = bookImageUrl,
            title = title,
            review = review,
            rating = rating
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
