package com.example.bookmeter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentMyReviewsBinding
import com.example.bookmeter.model.Post
import com.example.bookmeter.repository.PostRepository
import com.example.bookmeter.ui.adapters.PostAdapter
import com.example.bookmeter.ui.adapters.PostWithUser
import com.example.bookmeter.utils.LoadingStateManager
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.viewmodels.AuthViewModel
import com.example.bookmeter.viewmodels.MyReviewsViewModel
import com.example.bookmeter.viewmodels.PostListViewModel

class MyReviewsFragment : Fragment() {
    private var _binding: FragmentMyReviewsBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val myReviewsViewModel: MyReviewsViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter
    private lateinit var loadingStateManager: LoadingStateManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyReviewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize loading state manager
        loadingStateManager = LoadingStateManager(this)
        loadingStateManager.init(binding.root, binding.myReviewsContentArea.id)
        
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        loadMyReviews()
        
        // Setup swipe to refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshReviews()
        }
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            onEditClick = { post -> handlePostEdit(post) },
            onDeleteClick = { post -> handlePostDelete(post) },
            onLikeClick = { post -> handlePostLike(post) },
            onPostClick = { post -> navigateToPostDetail(post) },
            currentUserId = authViewModel.currentUser?.value?.uid,
            postViewModel = null // This is optional, can be null
        )
        
        binding.postsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = postAdapter
            setHasFixedSize(true)
            
            // Add animation for scrolling
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        // Scrolling down - hide FAB
                        binding.fabAddPost.hide()
                    } else if (dy < 0) {
                        // Scrolling up - show FAB
                        binding.fabAddPost.show()
                    }
                }
            })
        }
        
        // Show shimmer loading effect while posts are loading
        showRecyclerViewLoading(true)
    }

    private fun showRecyclerViewLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.postsRecyclerView.visibility = View.GONE
            binding.shimmerFrameLayout.visibility = View.VISIBLE
            binding.shimmerFrameLayout.startShimmer()
            binding.emptyStateTextView.visibility = View.GONE
        } else {
            binding.shimmerFrameLayout.stopShimmer()
            binding.shimmerFrameLayout.visibility = View.GONE
            binding.postsRecyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun setupObservers() {
        // Observe loading state
        myReviewsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                showRecyclerViewLoading(true)
            } else {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
        
        // Observe user posts
        myReviewsViewModel.userPosts.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    val posts = it.getOrNull() ?: emptyList()
                    
                    // Convert to PostWithUser objects
                    val postWithUsers = posts.map { post ->
                        PostWithUser(post, null) // Set user to null, as we're displaying our own posts
                    }
                    
                    // Submit the list
                    postAdapter.submitList(postWithUsers)
                    
                    // Show empty state if needed
                    binding.emptyStateTextView.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
                } else {
                    SnackbarHelper.showError(binding.root, "Failed to load your reviews: ${it.exceptionOrNull()?.message}")
                }
                
                // Always hide loading indicators when data arrives
                showRecyclerViewLoading(false)
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddPost.setOnClickListener {
            findNavController().navigate(R.id.action_myReviewsFragment_to_addPostFragment)
        }
    }
    
    private fun loadMyReviews() {
        val userId = authViewModel.currentUser?.value?.uid
        if (userId != null) {
            // Show shimmer loading effect
            showRecyclerViewLoading(true)
            myReviewsViewModel.getUserPosts(userId)
        } else {
            // User not logged in, show error
            SnackbarHelper.showInfo(binding.root, "You need to be logged in to view your reviews")
            findNavController().navigateUp()
        }
    }
    
    private fun refreshReviews() {
        val userId = authViewModel.currentUser?.value?.uid
        if (userId != null) {
            myReviewsViewModel.refreshUserPosts(userId)
        } else {
            binding.swipeRefreshLayout.isRefreshing = false
        }
    }
    
    private fun handlePostEdit(post: Post) {
        val action = MyReviewsFragmentDirections.actionMyReviewsFragmentToEditPostFragment(post.id)
        findNavController().navigate(action)
    }

    private fun handlePostDelete(post: Post) {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Review")
            .setMessage("Are you sure you want to delete this review? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deletePost(post)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deletePost(post: Post) {
        // Show loading state
        loadingStateManager.showLoading("Deleting review...")
        
        // Use the repository to delete the post
        val postRepository = PostRepository()
        
        // Call the delete function from repository
        postRepository.deletePost(post.id)
            .addOnSuccessListener {
                // Hide loading and show success message
                loadingStateManager.hideLoading()
                SnackbarHelper.showSuccess(binding.root, "Review deleted successfully")
                
                // Refresh the posts list
                refreshReviews()
            }
            .addOnFailureListener { e ->
                // Hide loading and show error message
                loadingStateManager.hideLoading()
                SnackbarHelper.showError(binding.root, "Failed to delete review: ${e.message}")
            }
    }
    
    private fun handlePostLike(post: Post) {
        val currentUserId = authViewModel.currentUser?.value?.uid
        if (currentUserId == null) {
            SnackbarHelper.showInfo(binding.root, "You need to be logged in to like reviews")
            return
        }
        
        // Use the post repository directly
        val postRepository = PostRepository()
        postRepository.toggleLike(post.id, currentUserId)
            .addOnSuccessListener {
                // Refresh the posts to reflect changes
                refreshReviews()
            }
            .addOnFailureListener { e ->
                SnackbarHelper.showError(binding.root, "Failed to update like: ${e.message}")
            }
    }
    
    private fun navigateToPostDetail(post: Post) {
        val action = MyReviewsFragmentDirections.actionMyReviewsFragmentToPostDetailFragment(post.id)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
