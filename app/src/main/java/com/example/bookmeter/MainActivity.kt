package com.example.bookmeter

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.bookmeter.databinding.ActivityMainBinding
import com.example.bookmeter.viewmodels.AuthViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val authViewModel: AuthViewModel by viewModels()

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
    }

    private fun observeAuthState() {
        // Check if user is already logged in via Room
        authViewModel.localUser.observe(this) { localUser ->
            val currentDestId = navController.currentDestination?.id
            
            if (localUser != null) {
                // User exists in Room, ensure we're on dashboard if not already
                if (currentDestId == R.id.loginFragment) {
                    navController.navigate(R.id.action_loginFragment_to_dashboardFragment)
                } else if (currentDestId == R.id.registerFragment) {
                    navController.navigate(R.id.action_registerFragment_to_dashboardFragment)
                }
            } else {
                // If no user in Room and we're on dashboard, go to login
                if (currentDestId == R.id.dashboardFragment) {
                    navController.navigate(R.id.action_dashboardFragment_to_loginFragment)
                }
            }
        }

        // Observe Firebase authentication state as backup
        authViewModel.currentUser.observe(this) { firebaseUser ->
            // Firebase state changes will ultimately update Room via the ViewModel
            // So we primarily rely on the localUser observer above
        }
    }
}