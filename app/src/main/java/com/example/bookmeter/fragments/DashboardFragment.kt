package com.example.bookmeter.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookmeter.MainActivity
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentDashboardBinding
import com.example.bookmeter.model.Post
import com.example.bookmeter.ui.adapters.PostAdapter
import com.example.bookmeter.utils.LoadingStateManager
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.viewmodels.AuthViewModel
import com.example.bookmeter.viewmodels.PostListViewModel
import com.google.android.material.navigation.NavigationView

class DashboardFragment : Fragment(), NavigationView.OnNavigationItemSelectedListener {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private val postListViewModel: PostListViewModel by viewModels()
    private val args: DashboardFragmentArgs by navArgs()
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var loadingStateManager: LoadingStateManager
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true) // Let the fragment handle menu events
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize loading state
        loadingStateManager = LoadingStateManager(this)
        loadingStateManager.init(binding.root, binding.dashboardContentArea.id)
        loadingStateManager.showLoading("Loading your dashboard...")

        setupToolbar()
        setupNavigationDrawer()
        setupPostsRecyclerView()
        setupSwipeRefresh()
        setupObservers()
        setupClickListeners()
        
        // Show success message if coming from login or registration
        if (args.showSnackbar) {
            SnackbarHelper.showSuccess(binding.root, "Login successful!")
        }
    }

    private fun setupToolbar() {
        val activity = requireActivity() as AppCompatActivity
        activity.setSupportActionBar(binding.toolbar)
    }

    private fun setupNavigationDrawer() {
        // Link drawer layout to MainActivity for proper back button handling
        (activity as? MainActivity)?.setDrawerLayout(binding.drawerLayout)
        
        toggle = ActionBarDrawerToggle(
            requireActivity(),
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)
    }
    
    private fun setupPostsRecyclerView() {
        postAdapter = PostAdapter(
            onEditClick = { post -> handlePostEdit(post) },
            onDeleteClick = { post -> handlePostDelete(post) },
            onLikeClick = { post -> handlePostLike(post) },
            onPostClick = { post -> navigateToPostDetail(post) },
            currentUserId = authViewModel.currentUser?.value?.uid,
            postViewModel = postListViewModel
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
            postListViewModel.refreshPosts()
        }
        
        // Set refresh indicator colors
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent,
            R.color.colorPrimaryDark
        )
    }

    private fun setupObservers() {
        // Observe user data
        authViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                // Update main content
                binding.userNameTextView.text = "Hello, ${user.name}!"
                
                // Update nav drawer header
                val headerView = binding.navigationView.getHeaderView(0)
                headerView.findViewById<android.widget.TextView>(R.id.navHeaderUsername).text = user.name
                headerView.findViewById<android.widget.TextView>(R.id.navHeaderEmail).text = user.email
                
                val profileImageView = headerView.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.navHeaderProfileImage)
                if (user.profilePictureUrl.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(user.profilePictureUrl)
                        .placeholder(R.drawable.profile_placeholder)
                        .error(R.drawable.profile_placeholder)
                        .into(profileImageView)
                }
            }
        }

        // Fallback to local user if Firestore data is not available
        authViewModel.localUser.observe(viewLifecycleOwner) { localUser ->
            if (localUser != null && authViewModel.user.value == null) {
                // Update with minimal data if Firebase data isn't available
                binding.userNameTextView.text = "Hello, ${localUser.name}!"
                
                // Update nav drawer header with minimal data
                val headerView = binding.navigationView.getHeaderView(0)
                headerView.findViewById<android.widget.TextView>(R.id.navHeaderUsername).text = localUser.name
                headerView.findViewById<android.widget.TextView>(R.id.navHeaderEmail).text = ""
            }
            
            // Check authentication status
            if (localUser == null && isAdded && _binding != null) {
                try {
                    // Only navigate if we're currently on the dashboard
                    val currentDestId = findNavController().currentDestination?.id
                    if (currentDestId == R.id.dashboardFragment) {
                        // Use simple navigation to login - let MainActivity handle back stack
                        findNavController().navigate(R.id.loginFragment)
                    }
                } catch (e: Exception) {
                    // Log the error but don't crash
                    android.util.Log.e("DashboardFragment", "Navigation error: ${e.message}")
                }
            }
        }

        // Observe post data
        postListViewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
            loadingStateManager.hideLoading()
            binding.swipeRefreshLayout.isRefreshing = false
            showRecyclerViewLoading(false)
            
            // Show empty state if needed
            binding.emptyStateTextView.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
        }
        
        // Observe post loading state
        postListViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                if (postAdapter.itemCount == 0) {
                    showRecyclerViewLoading(true)
                }
            }
        }
        
        // Observe post loading errors
        postListViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                binding.swipeRefreshLayout.isRefreshing = false
                SnackbarHelper.showError(binding.root, errorMessage)
            }
        }

        // Observe like action result
        postListViewModel.likeActionResult.observe(viewLifecycleOwner) { (postId, success) ->
            // Find the post view holder and update loading state
            val position = postAdapter.currentList.indexOfFirst { it.post.id == postId }
            if (position != -1) {
                val viewHolder = binding.postsRecyclerView.findViewHolderForAdapterPosition(position)
                if (viewHolder is PostAdapter.PostViewHolder) {
                    viewHolder.showLikeLoading(postId, false)
                }
                
                if (!success) {
                    SnackbarHelper.showError(binding.root, "Failed to update like")
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddPost.setOnClickListener {
            findNavController().navigate(R.id.action_dashboardFragment_to_addPostFragment)
        }
    }
    
    private fun handlePostEdit(post: Post) {
        // TODO: Navigate to edit post screen
        SnackbarHelper.showInfo(binding.root, "Edit post feature coming soon!")
    }

    private fun handlePostDelete(post: Post) {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Post")
            .setMessage("Are you sure you want to delete this post? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                // TODO: Implement delete post functionality
                SnackbarHelper.showInfo(binding.root, "Delete post feature coming soon!")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun handlePostLike(post: Post) {
        val currentUserId = authViewModel.currentUser?.value?.uid
        if (currentUserId == null) {
            SnackbarHelper.showInfo(binding.root, "You need to be logged in to like posts")
            return
        }
        
        // No need for manual visual feedback - this will be handled by the adapter
        
        // Toggle the like in the database
        postListViewModel.toggleLike(post.id, currentUserId)
    }
    
    private fun handlePostComment(post: Post) {
        // TODO: Implement comment functionality
        SnackbarHelper.showInfo(binding.root, "Comment feature coming soon!")
    }
    
    
    private fun navigateToPostDetail(post: Post) {
        val action = DashboardFragmentDirections.actionDashboardFragmentToPostDetailFragment(post.id)
        findNavController().navigate(action)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.dashboard_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                SnackbarHelper.showInfo(binding.root, "Search feature coming soon!")
                true
            }
            R.id.action_add -> {
                findNavController().navigate(R.id.action_dashboardFragment_to_addPostFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.dashboardFragment -> {
                // We're already on the home screen
            }
            R.id.profileFragment -> {
                findNavController().navigate(R.id.action_dashboardFragment_to_profileFragment)
            }
            R.id.addPostFragment -> {
                findNavController().navigate(R.id.action_dashboardFragment_to_addPostFragment)
            }
            R.id.nav_logout -> {
                // Close drawer immediately
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                
                // Show loading
                loadingStateManager.showLoading("Logging out...")
                
                // Perform logout
                authViewModel.logout { success, message ->
                    // Check if fragment is still attached
                    if (!isAdded || _binding == null) return@logout
                    
                    if (!success) {
                        loadingStateManager.hideLoading()
                        SnackbarHelper.showError(binding.root, "Logout failed: $message")
                    }
                    // Don't navigate here - navigation will be handled automatically
                }
                return true
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear drawer reference when fragment is destroyed
        (activity as? MainActivity)?.setDrawerLayout(null)
        _binding = null
    }
}