package com.example.bookmeter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentProfileBinding
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.utils.LoadingStateManager
import com.example.bookmeter.viewmodels.AuthViewModel

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private val args: ProfileFragmentArgs by navArgs()
    private lateinit var loadingStateManager: LoadingStateManager
    private var isDataLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize loading state manager
        loadingStateManager = LoadingStateManager(this)
        loadingStateManager.init(binding.root, R.id.profileContent)
        
        isDataLoaded = false
        loadingStateManager.showLoading("Loading your profile...")

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
            if (localUser == null && isAdded && _binding != null) {
                try {
                    // Only navigate if we're currently on the profile fragment
                    val currentDestId = findNavController().currentDestination?.id
                    if (currentDestId == R.id.profileFragment) {
                        // Navigate directly to login instead of using an action that may cause issues
                        findNavController().navigate(R.id.loginFragment)
                    }
                } catch (e: Exception) {
                    // Log the error but don't crash
                    android.util.Log.e("ProfileFragment", "Navigation error: ${e.message}")
                }
            }
        }
    }

    private fun setupObservers() {
        // Use Firestore user data when available
        authViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                // סימון שהנתונים נטענו כדי למנוע טעינה כפולה מ-Room
                isDataLoaded = true
                
                // Set user name and email
                binding.tvWelcome.text = "Welcome, ${user.name}!"
                binding.tvEmail.text = user.email
                
                // Load profile image if available
                if (user.profilePictureUrl.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(user.profilePictureUrl)
                        .placeholder(R.drawable.profile_placeholder)
                        .error(R.drawable.profile_placeholder)
                        .into(binding.profileImageView)
                }
                
                // Set book statistics
                binding.tvReadBooksCount.text = user.readBooks.size.toString()
                binding.tvWishlistCount.text = user.wishlistBooks.size.toString()
                
                // הסתרת מסך הטעינה רק אחרי שהכל מוכן להצגה
                loadingStateManager.hideLoading()
            }
        }

        // Fallback to local user if Firestore data is not available
        authViewModel.localUser.observe(viewLifecycleOwner) { userEntity ->
            // טיפול בנתוני Room רק אם אין נתונים מ-Firestore ולא טענו עדיין נתונים
            if (userEntity != null && !isDataLoaded && authViewModel.user.value == null) {
                val userName = userEntity.name
                binding.tvWelcome.text = "Welcome, $userName!"
                
                // בשימוש בנתונים מקומיים אין לנו שדה אימייל, לכן נציג שדה ריק
                binding.tvEmail.text = ""
                
                // Set default values for other UI elements
                binding.tvReadBooksCount.text = "0"
                binding.tvWishlistCount.text = "0"
                
                // סימון שטענו נתונים והסתרת מסך טעינה
                isDataLoaded = true
                loadingStateManager.hideLoading()
            }
        }

        // Observe loading state
        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnLogout.isEnabled = !isLoading
            
            if (isLoading) {
                loadingStateManager.showLoading("Loading your data...")
                // לא מסתירים את מסך הטעינה כאן - זה ייעשה רק כשהנתונים יגיעו
            }
        }
    }
    
    private fun setupClickListeners() {
        binding.btnEdit.setOnClickListener {
            if (isAdded && findNavController().currentDestination?.id == R.id.profileFragment) {
                val action = ProfileFragmentDirections.actionProfileFragmentToEditProfileFragment()
                findNavController().navigate(action)
            }
        }
        
        binding.btnLogout.setOnClickListener {
            // Disable the button immediately to prevent multiple clicks
            binding.btnLogout.isEnabled = false
            
            // Show loading indicator
            loadingStateManager.showLoading("Logging out...")
            
            authViewModel.logout { success, message ->
                // Check if fragment is still attached before updating UI
                if (!isAdded || _binding == null) return@logout

                // Re-enable button if there was an error
                if (!success) {
                    binding.btnLogout.isEnabled = true
                    loadingStateManager.hideLoading()
                    SnackbarHelper.showError(binding.root, "Logout failed: $message")
                }
                // Do NOT navigate here - navigation will be handled by observeAuthState in MainActivity
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}