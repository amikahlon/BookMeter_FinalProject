package com.example.bookmeter

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.bookmeter.databinding.ActivityMainBinding
import com.example.bookmeter.fragments.DashboardFragmentDirections
import com.example.bookmeter.fragments.LoginFragmentDirections
import com.example.bookmeter.fragments.RegisterFragmentDirections
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
        authViewModel.currentUser.observe(this) { user ->
            if (!::navController.isInitialized || navController.currentDestination == null) return@observe

            val currentDestId = navController.currentDestination?.id ?: return@observe

            if (user != null) {
                // המשתמש מחובר, מונעים ניווט נוסף אם כבר במסך המתאים
                if ((currentDestId == R.id.loginFragment || currentDestId == R.id.registerFragment) && currentDestId != R.id.dashboardFragment) {
                    try {
                        val userName = authViewModel.user.value?.name ?: "User"
                        val action = when (currentDestId) {
                            R.id.loginFragment -> LoginFragmentDirections.actionLoginFragmentToDashboardFragment("demo")
                            R.id.registerFragment -> RegisterFragmentDirections.actionRegisterFragmentToDashboardFragment("demo")
                            else -> null
                        }
                        action?.let { navController.navigate(it) }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error navigating: ${e.message}", e)
                    }
                }
            } else {
                // המשתמש לא מחובר, מונעים ניווט נוסף אם כבר במסך המתאים
                if (currentDestId == R.id.dashboardFragment) {
                    try {
                        val action = DashboardFragmentDirections.actionDashboardFragmentToLoginFragment()
                        navController.navigate(action)
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error navigating: ${e.message}", e)
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
