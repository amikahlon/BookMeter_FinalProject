package com.example.bookmeter.ui.editpost

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentEditPostBinding
import com.example.bookmeter.model.Post
import com.example.bookmeter.utils.PermissionHelper
import com.example.bookmeter.viewmodels.PostViewModel
import kotlin.math.roundToInt

class EditPostFragment : Fragment() {

    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!

    private lateinit var postViewModel: PostViewModel
    private val args: EditPostFragmentArgs by navArgs()

    private var postImageUri: Uri? = null
    private var currentPost: Post? = null
    private var hasChangedImage = false
    private var isRemovingImage = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openImagePicker()
        } else {
            Toast.makeText(context, "Permission is needed to select an image", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                postImageUri = uri
                hasChangedImage = true
                loadImageToView(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postViewModel = ViewModelProvider(this)[PostViewModel::class.java]

        setupToolbar()
        setupObservers()
        setupClickListeners()

        postViewModel.loadPostForEdit(args.postId)
    }

    private fun setupToolbar() {
        binding.toolbar.title = "Edit Review"
        binding.toolbar.setNavigationOnClickListener {
            if (hasUserMadeChanges()) {
                showDiscardChangesDialog()
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun hasUserMadeChanges(): Boolean {
        if (currentPost == null) return false
        if (hasChangedImage) return true
        val titleChanged = binding.editTitle.text.toString() != currentPost?.title
        val reviewChanged = binding.editReview.text.toString() != currentPost?.review
        val ratingChanged = binding.ratingBar.rating.roundToInt() != currentPost?.rating
        return titleChanged || reviewChanged || ratingChanged
    }

    private fun showDiscardChangesDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Discard Changes")
            .setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setPositiveButton("Discard") { _, _ ->
                findNavController().navigateUp()
            }
            .setNegativeButton("Keep Editing", null)
            .show()
    }

    private fun setupObservers() {
        postViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnUpdatePost.isEnabled = !isLoading
            binding.contentGroup.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
        }

        postViewModel.postToEdit.observe(viewLifecycleOwner) { post ->
            currentPost = post
            if (post != null) {
                populatePostDetails(post)
            } else {
                Toast.makeText(context, "Failed to load post", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }

        postViewModel.editPostResult.observe(viewLifecycleOwner) { success ->
            success?.let {
                if (it) {
                    Toast.makeText(context, "Review updated successfully", Toast.LENGTH_SHORT).show()
                    val action = EditPostFragmentDirections.actionEditPostFragmentToPostDetailFragment(args.postId)
                    findNavController().navigate(action)
                } else {
                    Toast.makeText(context, "Failed to update review", Toast.LENGTH_LONG).show()
                }
                postViewModel.resetEditPostResult()
            }
        }
    }

    private fun populatePostDetails(post: Post) {
        binding.bookTitle.text = post.bookName
        binding.editTitle.setText(post.title)
        binding.editReview.setText(post.review)
        binding.ratingBar.rating = post.rating.toFloat()

        if (post.bookImageUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(post.bookImageUrl)
                .placeholder(R.drawable.ic_book_placeholder)
                .into(binding.bookCoverImage)
        }

        if (post.imageUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(post.imageUrl)
                .into(binding.postImageView)
            binding.postImageView.visibility = View.VISIBLE
            binding.textNoImage.visibility = View.GONE
            binding.btnRemoveImage.visibility = View.VISIBLE
        } else {
            binding.postImageView.visibility = View.GONE
            binding.textNoImage.visibility = View.VISIBLE
            binding.btnRemoveImage.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnUpdatePost.setOnClickListener { submitUpdatedPost() }
        binding.btnSelectImage.setOnClickListener { checkPermissionAndOpenImagePicker() }
        binding.btnRemoveImage.setOnClickListener { removePostImage() }
    }

    private fun checkPermissionAndOpenImagePicker() {
        if (PermissionHelper.hasStoragePermission(requireContext())) {
            openImagePicker()
        } else {
            requestPermissionLauncher.launch(PermissionHelper.getStoragePermission())
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun loadImageToView(uri: Uri) {
        binding.postImageView.visibility = View.VISIBLE
        binding.textNoImage.visibility = View.GONE
        binding.btnRemoveImage.visibility = View.VISIBLE
        Glide.with(requireContext()).load(uri).into(binding.postImageView)
    }

    private fun removePostImage() {
        postImageUri = null
        hasChangedImage = true
        isRemovingImage = true  // Make sure this flag is set to true when removing image
        
        binding.postImageView.visibility = View.GONE
        binding.textNoImage.visibility = View.VISIBLE
        binding.btnRemoveImage.visibility = View.GONE
    }

    private fun submitUpdatedPost() {
        val title = binding.editTitle.text.toString().trim()
        val review = binding.editReview.text.toString().trim()
        val rating = binding.ratingBar.rating.roundToInt()
        
        // Validate input
        if (!validateInput(title, review, rating)) {
            return
        }
        
        // Determine image settings
        val imageUri = if (hasChangedImage && !isRemovingImage) postImageUri else null
        
        // Call updatePost with the correct shouldRemoveImage flag
        postViewModel.updatePost(
            postId = args.postId,
            title = title,
            review = review,
            rating = rating,
            newImageUri = imageUri,
            shouldRemoveImage = isRemovingImage
        )
    }

    /**
     * Validates the form input
     * @return true if all inputs are valid, false otherwise
     */
    private fun validateInput(title: String, review: String, rating: Int): Boolean {
        var isValid = true
        
        if (title.isEmpty()) {
            binding.titleLayout.error = "Title is required"
            isValid = false
        } else {
            binding.titleLayout.error = null
        }
        
        if (review.isEmpty()) {
            binding.reviewLayout.error = "Review is required"
            isValid = false
        } else {
            binding.reviewLayout.error = null
        }
        
        if (rating == 0) {
            // Animate the rating bar to draw attention
            binding.ratingBar.animate()
                .scaleX(1.2f).scaleY(1.2f)
                .setDuration(200)
                .withEndAction {
                    binding.ratingBar.animate()
                        .scaleX(1f).scaleY(1f)
                        .setDuration(200)
                }
            Toast.makeText(context, "Please provide a rating", Toast.LENGTH_SHORT).show()
            isValid = false
        }
        
        return isValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
