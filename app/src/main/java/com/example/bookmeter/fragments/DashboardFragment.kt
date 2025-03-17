package com.example.bookmeter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentDashboardBinding
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.viewmodels.AuthViewModel

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private val args: DashboardFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkAuthentication()
        setupObservers()
        setupClickListeners()

        // Show success message if coming from registration or login
        if (args.showSnackbar) {
            SnackbarHelper.showSuccess(binding.root, "Login successful!")
        }
    }

    private fun checkAuthentication() {
        // If no user in Room, redirect to login
        authViewModel.localUser.observe(viewLifecycleOwner) { localUser ->
            if (localUser == null && isAdded) {
                if (findNavController().currentDestination?.id == R.id.dashboardFragment) {
                    findNavController().navigate(R.id.action_dashboardFragment_to_loginFragment)
                }
            }
        }
    }

    private fun setupObservers() {
        // Use either the Firestore user data or fallback to local user
        authViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                binding.tvWelcome.text = "Welcome, ${user.name}!"
            }
        }

        // Fallback to local user if Firestore data is not available
        if (authViewModel.user.value == null) {
            authViewModel.localUser.observe(viewLifecycleOwner) { userEntity ->
                if (userEntity != null && authViewModel.user.value == null) {
                    val userName = userEntity.name ?: "Guest"
                    binding.tvWelcome.text = "Welcome, $userName!"
                }
            }
        }

        // Observe loading state
        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnLogout.isEnabled = !isLoading
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            authViewModel.logout { success, message ->
                if (!isAdded) return@logout

                if (!success) {
                    SnackbarHelper.showError(binding.root, "Logout failed: $message")
                }
                // Navigation will be handled by observeAuthState in MainActivity or 
                // checkAuthentication() when Room user becomes null
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}