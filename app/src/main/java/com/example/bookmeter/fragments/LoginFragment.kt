package com.example.bookmeter.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentLoginBinding
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.viewmodels.AuthViewModel

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()

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

        setupObservers()
        setupClickListeners()
        checkLocalUser()
    }

    private fun checkLocalUser() {
        authViewModel.localUser.observe(viewLifecycleOwner) { localUser ->
            if (localUser != null && isAdded) {
                logLocalUserDetails(localUser)
                if (findNavController().currentDestination?.id == R.id.loginFragment) {
                    findNavController().navigate(R.id.action_loginFragment_to_dashboardFragment)
                }
            }
        }
    }

    private fun logLocalUserDetails(localUser: Any) {
        Log.d("LoginFragment", "Local User Details:\n$localUser")
    }

    private fun setupObservers() {
        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSignIn.isEnabled = !isLoading
            binding.btnSignUp.isEnabled = !isLoading
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
                    Log.d("LoginFragment", "Login successful, waiting for local user data...")
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
