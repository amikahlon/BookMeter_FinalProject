package com.example.bookmeter.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentEditProfileBinding
import com.example.bookmeter.utils.LoadingStateManager
import com.example.bookmeter.utils.PermissionHelper
import com.example.bookmeter.utils.SnackbarHelper
import com.example.bookmeter.viewmodels.AuthViewModel

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var loadingStateManager: LoadingStateManager
    private var profileImageUri: Uri? = null
    private var hasShownRationale = false
    
    // Register the activity result launchers
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                profileImageUri = uri
                binding.profileImageView.setImageURI(uri)
            }
        }
    }
    
    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // ...existing code...
    }
    
    // Settings launcher
    private val openSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // ...existing code...
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true) // For back button in toolbar
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // הסרנו את setupToolbar() כי אנחנו משתמשים ב-toolbar של ה-MainActivity
        
        // Initialize loading state manager
        loadingStateManager = LoadingStateManager(this)
        loadingStateManager.init(binding.root, R.id.editProfileContent)
        
        loadingStateManager.showLoading("Loading your profile data...")
        
        setupObservers()
        setupClickListeners()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            findNavController().navigateUp()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    private fun setupObservers() {
        authViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                // Fill in current user data
                binding.etName.setText(user.name)
                
                // Load current profile image
                if (user.profilePictureUrl.isNotEmpty()) {
                    Glide.with(requireContext())
                        .load(user.profilePictureUrl)
                        .placeholder(R.drawable.profile_placeholder)
                        .error(R.drawable.profile_placeholder)
                        .into(binding.profileImageView)
                }
                
                loadingStateManager.hideLoading()
            }
        }
        
        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                loadingStateManager.showLoading("Updating your profile...")
                binding.btnSave.isEnabled = false
                binding.btnCancel.isEnabled = false
            } else {
                loadingStateManager.hideLoading()
                binding.btnSave.isEnabled = true
                binding.btnCancel.isEnabled = true
            }
        }
    }
    
    private fun setupClickListeners() {
        // Image selection
        binding.btnSelectImage.setOnClickListener {
            if (PermissionHelper.hasStoragePermission(requireContext())) {
                openImagePicker()
            } else {
                requestPermissionLauncher.launch(PermissionHelper.READ_EXTERNAL_STORAGE_PERMISSION)
            }
        }
        
        // Save button
        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            
            if (name.isEmpty()) {
                binding.nameLayout.error = "Name is required"
                return@setOnClickListener
            } else {
                binding.nameLayout.error = null
            }
            
            authViewModel.updateUserProfile(
                name = name,
                imageUri = profileImageUri
            ) { success, message ->
                if (!isAdded || _binding == null) return@updateUserProfile
                
                if (success) {
                    // Navigate back to profile with success message
                    val action = EditProfileFragmentDirections
                        .actionEditProfileFragmentToProfileFragment()
                    findNavController().navigate(action)
                } else {
                    SnackbarHelper.showError(binding.root, message ?: "Update failed")
                }
            }
        }
        
        // Cancel button
        binding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }
    
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }
    
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
