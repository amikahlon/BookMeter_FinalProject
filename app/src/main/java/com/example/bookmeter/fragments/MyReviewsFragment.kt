package com.example.bookmeter.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
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
import com.example.bookmeter.utils.LoadingStateManager
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.viewmodels.AuthViewModel
import com.example.bookmeter.viewmodels.PostViewModel

class MyReviewsFragment : Fragment() {
    private var _binding: FragmentMyReviewsBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val postViewModel: PostViewModel by viewModels()
    private lateinit var loadingStateManager: LoadingStateManager
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyReviewsBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize loading state
        loadingStateManager = LoadingStateManager(this)
        loadingStateManager.init(binding.root, binding.myReviewsContentArea.id)
        
        setupToolbar()
        setupPostsRecyclerView()
        setupSwipeRefresh()
        setupObservers()
        setupClickListeners()
        
        // Load user's posts
        loadUserPosts()
    }

    private fun setupToolbar() {
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
        activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        activity.supportActionBar?.setDisplayShowHomeEnabled(true)
        activity.supportActionBar?.title = "My Reviews"
        
        // Add back button listener
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun setupPostsRecyclerView() {
        postAdapter = PostAdapter(
            onEditClick = { post -> handlePostEdit(post) },
            onDeleteClick = { post -> handlePostDelete(post) },
            onLikeClick = { post -> handlePostLike(post) },
            onPostClick = { post -> navigateToPostDetail(post) },
            currentUserId = authViewModel.currentUser?.value?.uid,
            postViewModel = null // We don't need the post list view model here
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
        } else {
            binding.shimmerFrameLayout.stopShimmer()
            binding.shimmerFrameLayout.visibility = View.GONE
            binding.postsRecyclerView.visibility = View.VISIBLE
        }
    }
    
    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadUserPosts()
        }
        
        // Set refresh indicator colors
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent,
            R.color.colorPrimaryDark
        )
    }

    private fun setupObservers() {
        // Observe loading state
        postViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                // Only use shimmer effect for loading state
                showRecyclerViewLoading(true)
            } else {
                // Only update swipe refresh and shimmer, not the loading manager
                binding.swipeRefreshLayout.isRefreshing = false
                showRecyclerViewLoading(false)
            }
        }
        
        // Observe user posts
        postViewModel.userPosts.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    val posts = it.getOrNull() ?: emptyList()
                    
                    // Convert to PostWithUser objects
                    val postWithUsers = posts.map { post ->
                        com.example.bookmeter.ui.adapters.PostWithUser(post, authViewModel.user.value)
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
    
    private fun loadUserPosts() {
        val userId = authViewModel.currentUser?.value?.uid
        if (userId != null) {
            // Show shimmer loading effect instead of loading dialog
            showRecyclerViewLoading(true)
            postViewModel.getUserPosts(userId)
        } else {
            // User not logged in, show error
            SnackbarHelper.showInfo(binding.root, "You need to be logged in to view your reviews")
            findNavController().navigateUp()
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
        try {
            // Show loading state with explicit message
            loadingStateManager.showLoading("Deleting review...")
            
            // Use the repository to delete the post
            val postRepository = PostRepository()
            
            // Call the delete function from repository
            postRepository.deletePost(post.id)
                .addOnSuccessListener {
                    try {
                        // Hide loading and show success message
                        loadingStateManager.hideLoading()
                    } catch (e: Exception) {
                        // Fallback if loading manager fails
                        android.util.Log.e("MyReviewsFragment", "Error hiding loading: ${e.message}")
                    }
                    
                    SnackbarHelper.showSuccess(binding.root, "Review deleted successfully")
                    
                    // Refresh the posts list
                    loadUserPosts()
                }
                .addOnFailureListener { e ->
                    try {
                        // Hide loading and show error message
                        loadingStateManager.hideLoading()
                    } catch (ex: Exception) {
                        // Fallback if loading manager fails
                        android.util.Log.e("MyReviewsFragment", "Error hiding loading: ${ex.message}")
                    }
                    
                    SnackbarHelper.showError(binding.root, "Failed to delete review: ${e.message}")
                }
        } catch (e: Exception) {
            // Fallback if loading manager fails
            SnackbarHelper.showError(binding.root, "Failed to start delete operation: ${e.message}")
            
            // Still attempt to delete
            val postRepository = PostRepository()
            postRepository.deletePost(post.id)
                .addOnSuccessListener {
                    SnackbarHelper.showSuccess(binding.root, "Review deleted successfully")
                    loadUserPosts()
                }
                .addOnFailureListener { e ->
                    SnackbarHelper.showError(binding.root, "Failed to delete review: ${e.message}")
                }
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
                loadUserPosts()
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
