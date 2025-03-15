package com.example.bookmeter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
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
    ): View? {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isAdded && _binding != null) {
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        binding.btnSignIn.setOnClickListener {
            val email = binding.etUsername.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString()?.trim() ?: ""

            binding.tvErrorMessage.visibility = View.GONE
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
            } else if (password.length < 6) {
                binding.passwordInputLayout.error = "Password must be at least 6 characters"
                isValid = false
            } else {
                binding.passwordInputLayout.error = null
            }

            if (!isValid) return@setOnClickListener

            authViewModel.loginUser(email, password) { success, message ->
                if (!success && isAdded && _binding != null) {
                    binding.tvErrorMessage.text = "Incorrect email or password"
                    binding.tvErrorMessage.visibility = View.VISIBLE
                    SnackbarHelper.showError(binding.root, "Incorrect email or password")
                } else {
                    if (_binding != null) {
                        binding.tvErrorMessage.visibility = View.GONE
                        SnackbarHelper.showSuccess(binding.root, "Login successful!")
                    }
                }
            }
        }

        binding.btnSignUp.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToRegisterFragment())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
