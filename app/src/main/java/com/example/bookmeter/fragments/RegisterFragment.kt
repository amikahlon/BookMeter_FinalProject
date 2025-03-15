package com.example.bookmeter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.bookmeter.databinding.FragmentRegisterBinding
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

        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isAdded) {
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

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

            if (!isValid) return@setOnClickListener

            authViewModel.registerUser(name, email, password) { success, message ->
                if (!isAdded || activity == null) return@registerUser

                requireActivity().runOnUiThread {
                    if (!success) {
                        binding.emailLayout.error = "Email is already in use"
                    } else {
                        // ניווט לדשבורד עם פרמטר `showSnackbar=true`
                        val action = RegisterFragmentDirections
                            .actionRegisterFragmentToDashboardFragment(name)
                            .setShowSnackbar(true)
                        findNavController().navigate(action)
                    }
                }
            }
        }

        binding.btnBackToLogin.setOnClickListener {
            if (isAdded) {
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
