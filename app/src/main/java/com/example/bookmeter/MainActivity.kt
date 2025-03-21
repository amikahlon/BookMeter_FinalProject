package com.example.bookmeter

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.bumptech.glide.Glide
import com.example.bookmeter.databinding.ActivityMainBinding
import com.example.bookmeter.databinding.NavHeaderBinding
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.viewmodels.AuthViewModel
import com.google.android.material.navigation.NavigationView
import android.view.Menu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.internal.NavigationMenuView
import android.os.Build
import android.view.View

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // התאמת חלונות המערכת לתמיכה בעיצוב מלא
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
        }

        setupToolbar()
        setupNavigation()
        observeAuthState()
        setupNavigationDrawerHeader()
        
        // Apply custom style to NavigationView
        setupNavigationDrawerStyle()
        
        // Always ensure our menu is shown
        binding.toolbar.inflateMenu(R.menu.main_menu)
        setupMenuButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupMenuButton() {
        // Add a menu button to the right side of the toolbar
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_menu -> {
                    // Open the drawer from the right side
                    binding.drawerLayout.openDrawer(GravityCompat.END)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Define top-level destinations - none will have a Back button
        // since we want to manually handle back navigation
        appBarConfiguration = AppBarConfiguration(emptySet())
        
        // Connect toolbar with navigation controller for title updates
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Custom navigation item handling
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.END)
                    
                    binding.root.post {
                        authViewModel.logout { success, message ->
                            if (!success) {
                                SnackbarHelper.showError(binding.root, "Logout failed: $message")
                            }
                            // Navigation will be handled by observer
                        }
                    }
                    true
                }
                else -> {
                    // Handle standard navigation
                    binding.drawerLayout.closeDrawer(GravityCompat.END)
                    navController.navigate(menuItem.itemId)
                    true
                }
            }
        }
        
        // Update drawer state based on destination changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Lock drawer for login/register screens, always show the MENU button on other screens
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    supportActionBar?.hide()
                    
                    // Set white status bar for login/register screens
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)
                        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    }
                }
                else -> {
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    supportActionBar?.show()
                    
                    // Restore colored status bar for all other screens
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        window.statusBarColor = ContextCompat.getColor(this, R.color.colorPrimary)
                        window.decorView.systemUiVisibility = 0 // Reset to default
                    }
                    
                    // Set up the back button on toolbar for non-top-level destinations
                    if (destination.id == R.id.dashboardFragment || 
                        destination.id == R.id.profileFragment || 
                        destination.id == R.id.myReviewsFragment) {
                        // Top-level destination - don't show back button
                        supportActionBar?.setDisplayHomeAsUpEnabled(false)
                    } else {
                        // Show back button for all other destinations
                        supportActionBar?.setDisplayHomeAsUpEnabled(true)
                        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
                    }
                    
                    // Update title based on current destination
                    supportActionBar?.title = destination.label
                    
                    // Forcefully ensure our menu is always shown
                    invalidateOptionsMenu()
                    binding.toolbar.menu.clear()
                    binding.toolbar.inflateMenu(R.menu.main_menu)
                }
            }
        }
    }
    
    private fun setupNavigationDrawerHeader() {
        val headerView = binding.navView.getHeaderView(0)
        val headerBinding = NavHeaderBinding.bind(headerView)
        
        // Observe user data to update drawer header
        authViewModel.user.observe(this) { user ->
            if (user != null) {
                headerBinding.navHeaderUsername.text = user.name
                headerBinding.navHeaderEmail.text = user.email
                
                if (user.profilePictureUrl.isNotEmpty()) {
                    Glide.with(this)
                        .load(user.profilePictureUrl)
                        .placeholder(R.drawable.profile_placeholder)
                        .error(R.drawable.profile_placeholder)
                        .into(headerBinding.navHeaderProfileImage)
                }
            }
        }
        
        // Fallback to local user if Firestore data is not available
        authViewModel.localUser.observe(this) { localUser -> 
            if (localUser != null && authViewModel.user.value == null) {
                headerBinding.navHeaderUsername.text = localUser.name
                headerBinding.navHeaderEmail.text = ""
            }
        }
    }

    private fun observeAuthState() {
        // Check if user is already logged in via Room
        authViewModel.localUser.observe(this) { localUser -> 
            try {
                val currentDestId = navController.currentDestination?.id ?: return@observe
                
                if (localUser != null) {
                    // User exists in Room, ensure we're on dashboard if not already
                    if (currentDestId == R.id.loginFragment) {
                        navController.navigate(R.id.action_loginFragment_to_dashboardFragment)
                    } else if (currentDestId == R.id.registerFragment) {
                        navController.navigate(R.id.action_registerFragment_to_dashboardFragment)
                    }
                } else {
                    // If no user in Room and we're on dashboard or profile, go to login
                    if (currentDestId == R.id.dashboardFragment || 
                        currentDestId == R.id.profileFragment ||
                        currentDestId == R.id.myReviewsFragment) {
                        // Create explicit NavOptions with popUpTo
                        val navOptions = NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true) // true for inclusive
                            .build()
                        
                        // Navigate with explicit options - avoid navigating if we're already at login
                        if (currentDestId != R.id.loginFragment) {
                            navController.navigate(R.id.loginFragment, null, navOptions)
                        }
                    }
                }
            } catch (e: Exception) {
                // Log error but don't crash
                android.util.Log.e("MainActivity", "Navigation error: ${e.message}")
            }
        }
    }
    
    private fun setupNavigationDrawerStyle() {
        // Apply custom style to the navigation drawer
        val navigationView = binding.navView
        val menuView = navigationView.getChildAt(0) as? NavigationMenuView
        menuView?.let {
            it.addItemDecoration(
                DividerItemDecoration(this, DividerItemDecoration.VERTICAL).apply {
                    setDrawable(ContextCompat.getDrawable(this@MainActivity, R.drawable.nav_item_divider)!!)
                }
            )
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        // Handle back button press in toolbar
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
    
    // Handle back button presses for navigation drawer
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }

    // Add this method to make sure our menu is shown
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
}