package com.example.bookmeter.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentPostDetailBinding
import com.example.bookmeter.model.Post
import com.example.bookmeter.model.User
import com.example.bookmeter.repository.PostRepository
import com.example.bookmeter.utils.LoadingStateManager
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.viewmodels.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class PostDetailFragment : Fragment() {
    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!
    
    private val args: PostDetailFragmentArgs by navArgs()
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var loadingStateManager: LoadingStateManager
    
    private val firestore = FirebaseFirestore.getInstance()
    private var currentPost: Post? = null
    private var postUser: User? = null

    private val postRepository = PostRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize loading state with both root view and content view ID
        loadingStateManager = LoadingStateManager(this)
        loadingStateManager.init(binding.root, binding.fragmentPostDetailContent.id)
        loadingStateManager.showLoading("Loading post...")
        
        setupToolbar()
        loadPostDetails()
        setupClickListeners()
    }
    
    private fun setupToolbar() {
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity.supportActionBar?.setDisplayShowHomeEnabled(true)
        activity.supportActionBar?.title = "Book Review"
        
        // Add back button listener
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        
        setHasOptionsMenu(true)
    }
    
    private fun loadPostDetails() {
        firestore.collection("posts").document(args.postId)
            .get()
            .addOnSuccessListener { document ->
                val post = document.toObject(Post::class.java)
                if (post != null) {
                    currentPost = post
                    displayPostDetails(post)
                    
                    // Load user data for the post
                    firestore.collection("users").document(post.userId)
                        .get()
                        .addOnSuccessListener { userDoc ->
                            postUser = userDoc.toObject(User::class.java)
                            postUser?.let { user ->
                                binding.userName.text = user.name
                                
                                if (user.profilePictureUrl.isNotEmpty()) {
                                    Glide.with(requireContext())
                                        .load(user.profilePictureUrl)
                                        .placeholder(R.drawable.profile_placeholder)
                                        .into(binding.userProfileImage)
                                }
                            }
                            
                            loadingStateManager.hideLoading()
                        }
                        .addOnFailureListener {
                            loadingStateManager.hideLoading()
                            SnackbarHelper.showError(binding.root, "Failed to load user details")
                        }
                } else {
                    loadingStateManager.hideLoading()
                    SnackbarHelper.showError(binding.root, "Post not found")
                    findNavController().navigateUp()
                }
            }
            .addOnFailureListener {
                loadingStateManager.hideLoading()
                SnackbarHelper.showError(binding.root, "Failed to load post details")
                findNavController().navigateUp()
            }
    }
    
    private fun displayPostDetails(post: Post) {
        // Set post content
        binding.reviewTitle.text = post.title
        binding.reviewText.text = post.review
        binding.ratingBar.rating = post.rating.toFloat()
        binding.postTimestamp.text = formatTimestamp(post.timestamp)
        
        // Set book details
        binding.bookTitle.text = post.bookName
        
        if (post.bookImageUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(post.bookImageUrl)
                .placeholder(R.drawable.ic_book_placeholder)
                .into(binding.bookCoverImage)
        }
        
        // Handle post image
        if (post.imageUrl.isNotEmpty()) {
            binding.postImage.visibility = View.VISIBLE
            Glide.with(requireContext())
                .load(post.imageUrl)
                .into(binding.postImage)
        } else {
            binding.postImage.visibility = View.GONE
        }

        // Set like button state
        val currentUserId = authViewModel.currentUser?.value?.uid
        val isLiked = currentUserId != null && post.likedBy.contains(currentUserId)
        updateLikeButtonState(isLiked)
        
        // Format like count text
        val likeCount = post.likes
        binding.btnLike.text = when {
            likeCount == 0 -> "Like"
            likeCount == 1 -> "1 Like"
            else -> "$likeCount Likes"
        }
    }
    
    private fun setupClickListeners() {
        binding.btnLike.setOnClickListener {
            handleLikeAction()
        }
        
        binding.btnShare.setOnClickListener {
            sharePost()
        }
        
        // Change the options menu to show/hide edit/delete buttons based on ownership
        val currentUserId = authViewModel.currentUser?.value?.uid
        currentPost?.let { post ->
            if (post.userId == currentUserId) {
                // Add toolbar menu items for edit and delete
                val toolbar = binding.toolbar
                toolbar.inflateMenu(R.menu.post_detail_owner_menu)
                
                toolbar.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_edit_post -> {
                            SnackbarHelper.showInfo(binding.root, "Edit post feature coming soon!")
                            true
                        }
                        R.id.action_delete_post -> {
                            showDeleteConfirmation()
                            true
                        }
                        else -> false
                    }
                }
            }
        }
    }

    private fun handleLikeAction() {
        val currentUserId = authViewModel.currentUser?.value?.uid
        if (currentUserId == null) {
            SnackbarHelper.showInfo(binding.root, "You need to be logged in to like posts")
            return
        }
        
        currentPost?.let { post ->
            // Show loading state
            setLikeButtonLoading(true)
            
            // Toggle the like in the database
            postRepository.toggleLike(post.id, currentUserId)
                .addOnSuccessListener {
                    // Refresh post data to get updated like count
                    loadPostDetails()
                }
                .addOnFailureListener { e ->
                    // Hide loading state
                    setLikeButtonLoading(false)
                    
                    // Show error
                    SnackbarHelper.showError(binding.root, "Failed to update like: ${e.message}")
                }
        }
    }

    private fun setLikeButtonLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.likeProgressBar.visibility = View.VISIBLE
            binding.btnLike.icon = null
            binding.btnLike.text = ""
            binding.btnLike.isEnabled = false
        } else {
            binding.likeProgressBar.visibility = View.GONE
            binding.btnLike.isEnabled = true
            // The icon and text will be set in the updateLikeButtonState method
        }
    }

    private fun updateLikeButtonState(isLiked: Boolean) {
        binding.likeProgressBar.visibility = View.GONE
        binding.btnLike.isEnabled = true
        binding.btnLike.isSelected = isLiked
        binding.btnLike.setIconResource(
            if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like_outline
        )
    }
    
    private fun showDeleteConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePost()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deletePost() {
        currentPost?.let { post ->
            loadingStateManager.showLoading("Deleting post...")
            
            firestore.collection("posts").document(post.id)
                .delete()
                .addOnSuccessListener {
                    loadingStateManager.hideLoading()
                    SnackbarHelper.showSuccess(binding.root, "Post deleted successfully")
                    findNavController().navigateUp()
                }
                .addOnFailureListener { e ->
                    loadingStateManager.hideLoading()
                    SnackbarHelper.showError(binding.root, "Failed to delete post: ${e.message}")
                }
        }
    }
    
    private fun sharePost() {
        currentPost?.let { post ->
            val shareText = """
                Check out this book review on BookMeter!
                
                "${post.title}" by ${postUser?.name ?: "a BookMeter user"}
                Book: ${post.bookName}
                Rating: ${post.rating}/5
                
                ${post.review}
            """.trimIndent()
            
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            
            startActivity(android.content.Intent.createChooser(shareIntent, "Share via"))
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        return format.format(date)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
