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
import com.example.bookmeter.databinding.FragmentDashboardBinding
import com.example.bookmeter.model.BookGenre
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.utils.LoadingStateManager
import com.example.bookmeter.viewmodels.AuthViewModel
import com.google.android.material.chip.Chip

class DashboardFragment : Fragment() {
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private val args: DashboardFragmentArgs by navArgs()
    private lateinit var loadingStateManager: LoadingStateManager
    private var isDataLoaded = false  // דגל שמציין אם הנתונים נטענו

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

        // Initialize loading state manager
        loadingStateManager = LoadingStateManager(this)
        loadingStateManager.init(binding.root, R.id.dashboardContent)
        
        // איפוס הדגל וטעינת מסך הטעינה בהתחלה
        isDataLoaded = false
        loadingStateManager.showLoading("Loading your dashboard...")

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
                
                // Display favorite genres
                setupGenreChips(user.favoriteGenres)
                
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
                
                // נציג רשימת ז'אנרים ריקה
                setupGenreChips(emptyList())
                
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
    
    private fun setupGenreChips(genres: List<String>) {
        binding.genresChipGroup.removeAllViews()
        
        if (genres.isEmpty()) {
            // Show a message if no genres are selected
            val chip = Chip(requireContext())
            chip.text = "No favorite genres selected"
            chip.isCheckable = false
            binding.genresChipGroup.addView(chip)
            return
        }
        
        // Convert genre enum names to display names
        val displayNames = BookGenre.toDisplayNames(genres)
        
        // Add a chip for each genre
        for (genreName in displayNames) {
            val chip = Chip(requireContext())
            chip.text = genreName
            chip.isClickable = false
            chip.isCheckable = false
            binding.genresChipGroup.addView(chip)
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