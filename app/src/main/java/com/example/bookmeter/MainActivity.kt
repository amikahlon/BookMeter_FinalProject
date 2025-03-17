package com.example.bookmeter

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.bookmeter.databinding.ActivityMainBinding
import com.example.bookmeter.viewmodels.AuthViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val authViewModel: AuthViewModel by viewModels()
    
    // Added for accessing drawer from fragments
    private var drawerLayout: DrawerLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        observeAuthState()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        
        // Set top-level destinations (won't show back button)
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.loginFragment, R.id.dashboardFragment)
        )
    }

    // Allow fragments to access the drawer
    fun setDrawerLayout(drawer: DrawerLayout?) {
        this.drawerLayout = drawer
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
                    if (currentDestId == R.id.dashboardFragment || currentDestId == R.id.profileFragment) {
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
        if (drawerLayout?.isOpen == true) {
            drawerLayout?.close()
        } else {
            super.onBackPressed()
        }
    }
}