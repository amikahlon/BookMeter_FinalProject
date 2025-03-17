package com.example.bookmeter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentLoginBinding
import com.example.bookmeter.utils.LoadingStateManager
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.viewmodels.AuthViewModel

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var loadingStateManager: LoadingStateManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize loading state manager
        loadingStateManager = LoadingStateManager(this)
        loadingStateManager.init(binding.root, R.id.loginContent)

        // Show initial loading while checking for user
        loadingStateManager.showLoading("Checking login status...")

        setupObservers()
        setupClickListeners()
        checkLocalUser()
    }

    private fun checkLocalUser() {
        // If there's a user in Room, navigate to dashboard immediately
        authViewModel.localUser.observe(viewLifecycleOwner) { localUser ->
            if (localUser != null && isAdded) {
                if (findNavController().currentDestination?.id == R.id.loginFragment) {
                    findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
                }
            } else {
                // No user found, hide loading screen
                loadingStateManager.hideLoading()
            }
        }
    }

    private fun setupObservers() {
        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                loadingStateManager.showLoading("Signing in...")
            } else {
                if (authViewModel.currentUser.value == null) {
                    loadingStateManager.hideLoading()
                }
            }
            
            // Disable buttons during loading
            binding.btnSignIn.isEnabled = !isLoading
            binding.btnSignUp.isEnabled = !isLoading
        }

        // Observe Firebase user
        authViewModel.currentUser.observe(viewLifecycleOwner) { firebaseUser ->
            if (firebaseUser != null && isAdded) {
                // Once Firebase auth is complete, the ViewModel will fetch user data 
                loadingStateManager.updateLoadingMessage("Loading your profile...")
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            val email = binding.etUsername.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString()?.trim() ?: ""

            binding.tvErrorMessage.visibility = View.GONE

            if (!validateInput(email, password)) return@setOnClickListener

            authViewModel.loginUser(email, password) { success, message -> 
                if (!isAdded) return@loginUser

                if (success) {
                    // Navigation will be handled by localUser observer in MainActivity
                    // or in checkLocalUser() once the user data is saved to Room
                } else {
                    binding.tvErrorMessage.text = message ?: "Login failed"
                    binding.tvErrorMessage.visibility = View.VISIBLE
                }
            }
        }

        binding.btnSignUp.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        var isValid = true

        if (email.isEmpty()) {
            binding.usernameInputLayout.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.usernameInputLayout.error = "Invalid email format"
            isValid = false
        } else {
            binding.usernameInputLayout.error = null
        }

        if (password.isEmpty()) {
            binding.passwordInputLayout.error = "Password is required"
            isValid = false
        } else {
            binding.passwordInputLayout.error = null
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}