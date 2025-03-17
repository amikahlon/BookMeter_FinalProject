package com.example.bookmeter.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentRegisterBinding
import com.example.bookmeter.model.BookGenre
import com.example.bookmeter.utils.PermissionHelper
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.viewmodels.AuthViewModel
import com.google.android.material.chip.Chip
import com.example.bookmeter.utils.LoadingStateManager

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()
    private var profileImageUri: Uri? = null
    private val selectedGenres = mutableListOf<String>()
    private lateinit var loadingStateManager: LoadingStateManager

    // Track whether we've shown the rationale dialog
    private var hasShownRationale = false

    // Register the activity result launchers during fragment initialization
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                profileImageUri = uri
                binding.profileImageView.setImageURI(uri)
            }
        }
    }
    
    // Permission request launcher - register early in fragment lifecycle
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        when {
            isGranted -> {
                // Permission granted, open image picker
                openImagePicker()
            }
            shouldShowRequestPermissionRationale(PermissionHelper.READ_EXTERNAL_STORAGE_PERMISSION) -> {
                // Permission denied but rationale can be shown
                if (!hasShownRationale) {
                    showPermissionRationaleDialog()
                    hasShownRationale = true
                } else {
                    SnackbarHelper.showError(binding.root, "Permission is needed to select an image")
                }
            }
            else -> {
                // Permission permanently denied, guide user to settings
                showOpenSettingsDialog()
            }
        }
    }

    // Settings launcher
    private val openSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Check permission again after returning from settings
        if (PermissionHelper.hasStoragePermission(requireContext())) {
            openImagePicker()
        }
    }

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
        
        // Initialize loading state manager
        loadingStateManager = LoadingStateManager(this)
        loadingStateManager.init(binding.root, R.id.registerContent)
        
        setupObservers()
        setupGenreChips()
        setupClickListeners()
        checkLocalUser()
    }

    private fun setupGenreChips() {
        val genres = BookGenre.values()
        binding.genresChipGroup.apply {
            removeAllViews()  // Clear any existing chips
            
            genres.forEach { genre ->
                val chip = layoutInflater.inflate(
                    R.layout.item_chip_choice, 
                    binding.genresChipGroup, 
                    false
                ) as Chip
                
                chip.text = genre.displayName
                chip.isCheckable = true
                
                chip.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedGenres.add(genre.name)
                    } else {
                        selectedGenres.remove(genre.name)
                    }
                }
                
                addView(chip)
            }
        }
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
            if (isLoading) {
                loadingStateManager.showLoading("Creating your account...")
            } else {
                loadingStateManager.hideLoading()
            }
            
            // Still update UI controls
            binding.btnRegister.isEnabled = !isLoading
            binding.btnBackToLogin.isEnabled = !isLoading
            binding.btnSelectImage.isEnabled = !isLoading
        }

        // Observe Firebase user creation
        authViewModel.currentUser.observe(viewLifecycleOwner) { firebaseUser ->
            if (firebaseUser != null) {
                loadingStateManager.updateLoadingMessage("Setting up your profile...")
            }
        }
    }

    private fun setupClickListeners() {
        // Image selection button
        binding.btnSelectImage.setOnClickListener {
            if (PermissionHelper.hasStoragePermission(requireContext())) {
                openImagePicker()
            } else {
                requestPermissionLauncher.launch(PermissionHelper.READ_EXTERNAL_STORAGE_PERMISSION)
            }
        }

        // Register button
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (!validateInput(name, email, password)) return@setOnClickListener

            authViewModel.registerUser(
                name, 
                email, 
                password, 
                profileImageUri,
                selectedGenres
            ) { success, message ->
                if (!isAdded || _binding == null) return@registerUser

                if (!success) {
                    SnackbarHelper.showError(binding.root, message ?: "Registration failed")
                }
                // Successful registration navigation handled by localUser observer
            }
        }

        // Back to login button
        binding.btnBackToLogin.setOnClickListener {
            if (isAdded && findNavController().currentDestination?.id == R.id.registerFragment) {
                findNavController().navigateUp()
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
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

    /**
     * Shows a dialog explaining why we need the permission
     */
    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage("We need access to your photos to select a profile picture.")
            .setPositiveButton("OK") { _, _ ->
                requestPermissionLauncher.launch(PermissionHelper.READ_EXTERNAL_STORAGE_PERMISSION)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Shows a dialog guiding the user to app settings
     */
    private fun showOpenSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage("Permission is needed to select a profile picture. Please grant storage access in settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = PermissionHelper.createAppSettingsIntent(requireContext())
                openSettingsLauncher.launch(intent)
            }
            .setNegativeButton("Not Now", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}