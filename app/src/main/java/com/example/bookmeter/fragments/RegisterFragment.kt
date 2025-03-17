package com.example.bookmeter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentRegisterBinding
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.viewmodels.AuthViewModel

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
        checkLocalUser()
    }

    private fun checkLocalUser() {
        // If user exists in Room, navigate directly to dashboard
        authViewModel.localUser.observe(viewLifecycleOwner) { localUser ->
            if (localUser != null && isAdded) {
                if (findNavController().currentDestination?.id == R.id.registerFragment) {
                    val action = RegisterFragmentDirections.actionRegisterFragmentToDashboardFragment()
                    findNavController().navigate(action)
                }
            }
        }
    }

    private fun setupObservers() {
        // Observe loading state
        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRegister.isEnabled = !isLoading
            binding.btnBackToLogin.isEnabled = !isLoading
        }

        // Observe Firebase user creation
        authViewModel.currentUser.observe(viewLifecycleOwner) { firebaseUser ->
            // User created successfully, will be saved to Room by the ViewModel
            // Navigation will be handled by localUser observer
        }
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (!validateInput(name, email, password)) return@setOnClickListener

            authViewModel.registerUser(name, email, password) { success, message ->
                if (!isAdded || _binding == null) return@registerUser

                if (!success) {
                    SnackbarHelper.showError(binding.root, message ?: "Registration failed")
                }
                // Successful registration navigation handled by localUser observer
            }
        }

        binding.btnBackToLogin.setOnClickListener {
            if (isAdded && findNavController().currentDestination?.id == R.id.registerFragment) {
                findNavController().navigateUp()
            }
        }
    }

    private fun validateInput(name: String, email: String, password: String): Boolean {
        if (!isAdded || _binding == null) return false

        var isValid = true

        if (name.isEmpty()) {
            binding.nameLayout.error = "Full name is required"
            isValid = false
        } else {
            binding.nameLayout.error = null
        }

        if (email.isEmpty()) {
            binding.emailLayout.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Invalid email format"
            isValid = false
        } else {
            binding.emailLayout.error = null
        }

        if (password.isEmpty()) {
            binding.passwordLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.passwordLayout.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.passwordLayout.error = null
        }

        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}