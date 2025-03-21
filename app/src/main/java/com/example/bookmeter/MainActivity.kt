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
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.bookmeter.databinding.ActivityMainBinding
import com.example.bookmeter.databinding.NavHeaderBinding
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.viewmodels.AuthViewModel
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupNavigation()
        observeAuthState()
        setupNavigationDrawerHeader()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // הגדרת המסכים הראשיים שלא יציגו כפתור חזרה
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.loginFragment,
                R.id.dashboardFragment,
                R.id.profileFragment,
                R.id.myReviewsFragment
            ),
            binding.drawerLayout
        )
        
        // חיבור ה-toolbar עם ה-navigation controller
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // חיבור תפריט המגירה עם ה-navigation controller
        binding.navView.setupWithNavController(navController)
        
        // טיפול מותאם אישית בפריטי תפריט המגירה
        binding.navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    
                    binding.root.post {
                        authViewModel.logout { success, message ->
                            if (!success) {
                                SnackbarHelper.showError(binding.root, "Logout failed: $message")
                            }
                            // הניווט יטופל על ידי ה-observer
                        }
                    }
                    true
                }
                else -> {
                    // טיפול בניווט סטנדרטי
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    navController.navigate(menuItem.itemId)
                    true
                }
            }
        }
        
        // עדכון מצב המגירה בהתאם לשינויים ביעד
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // נעילת המגירה במסכי התחברות/הרשמה
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> {
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    supportActionBar?.hide()
                }
                else -> {
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    supportActionBar?.show()
                    
                    // עדכון הכותרת בהתאם ליעד הנוכחי
                    supportActionBar?.title = destination.label
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
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
    
    // Handle back button presses for navigation drawer
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}