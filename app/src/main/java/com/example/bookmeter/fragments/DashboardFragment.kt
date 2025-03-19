package com.example.bookmeter.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.bookmeter.MainActivity
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentDashboardBinding
import com.example.bookmeter.utils.LoadingStateManager
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.viewmodels.AuthViewModel
import com.google.android.material.navigation.NavigationView

class DashboardFragment : Fragment(), NavigationView.OnNavigationItemSelectedListener {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private val args: DashboardFragmentArgs by navArgs()
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var loadingStateManager: LoadingStateManager

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
                
                // Hide loading when data is ready
                loadingStateManager.hideLoading()
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
                
                // Hide loading when local data is ready
                loadingStateManager.hideLoading()
            }
            
            // Check authentication status - FIX THE NAVIGATION ERROR
            // Don't try to navigate if we're not attached to a context or if view is destroyed
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

        // Observe loading state
        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                loadingStateManager.showLoading("Loading your data...")
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddBook.setOnClickListener {
            SnackbarHelper.showInfo(binding.root, "Add book feature coming soon!")
            // In a real app, you would navigate to add book screen
            // findNavController().navigate(R.id.action_dashboardFragment_to_addBookFragment)
        }
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
                SnackbarHelper.showInfo(binding.root, "Add book feature coming soon!")
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
            // If you have other menu items like wishlist, settings, etc.
            // Add them here using their correct resource IDs
            /*
            R.id.nav_wishlist -> {
                SnackbarHelper.showInfo(binding.root, "Wishlist feature coming soon!")
            }
            R.id.nav_settings -> {
                SnackbarHelper.showInfo(binding.root, "Settings feature coming soon!")
            }
            */
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