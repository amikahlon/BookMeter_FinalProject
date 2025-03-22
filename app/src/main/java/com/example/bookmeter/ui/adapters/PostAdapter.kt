package com.example.bookmeter.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookmeter.R
import com.example.bookmeter.databinding.ItemPostBinding
import com.example.bookmeter.model.Post
import com.example.bookmeter.model.User
import com.example.bookmeter.viewmodels.PostListViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostAdapter(
    private val onEditClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit,
    private val onLikeClick: (Post) -> Unit,
    private val onPostClick: (Post) -> Unit,
    private val currentUserId: String? = null,
    private val postViewModel: PostListViewModel? = null
) : ListAdapter<PostWithUser, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            // Set click listeners
            binding.btnViewDetails.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPostClick(getItem(position).post)
                }
            }
            
            binding.btnEditPost.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(getItem(position).post)
                }
            }
            
            binding.btnDeletePost.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position).post)
                }
            }
            
            binding.btnLike.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Show loading state
                    setLikeLoading(true)
                    onLikeClick(getItem(position).post)
                }
            }
            
            // Updated click listener for Add to List button
            binding.btnAddToList.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val post = getItem(position).post
                    addToWishlist(post.bookId)
                }
            }
        }

        private fun setLikeLoading(isLoading: Boolean) {
            if (isLoading) {
                binding.likeProgressBar.visibility = View.VISIBLE
                binding.btnLike.icon = null
                binding.btnLike.text = ""
                binding.btnLike.isEnabled = false
            } else {
                binding.likeProgressBar.visibility = View.GONE
                binding.btnLike.isEnabled = true
                // The icon and text will be set in the bind method
            }
        }

        fun bind(postWithUser: PostWithUser) {
            val post = postWithUser.post
            val user = postWithUser.user
            
            // Bind user info
            binding.userName.text = user?.name ?: "Unknown User"
            binding.postTimestamp.text = formatTimestamp(post.timestamp)
            
            // Check if current user is the post owner and show edit/delete buttons accordingly
            if (currentUserId != null && post.userId == currentUserId) {
                binding.ownerActionsContainer.visibility = View.VISIBLE
                binding.btnAddToList.visibility = View.GONE  // Hide add button for own posts
            } else {
                binding.ownerActionsContainer.visibility = View.GONE
                // Show Add to List button only for posts that are NOT from the current user
                binding.btnAddToList.visibility = if (currentUserId != null) View.VISIBLE else View.GONE
            }
            
            // Load user profile image
            val profileImageUrl = user?.profilePictureUrl.orEmpty()
            if (profileImageUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.profile_placeholder)
                    .into(binding.userProfileImage)
            } else {
                binding.userProfileImage.setImageResource(R.drawable.profile_placeholder)
            }
            
            // Bind book info
            binding.bookTitle.text = post.bookName
            binding.ratingBar.rating = post.rating.toFloat()
            binding.ratingText.text = post.rating.toString()
            
            // Load book cover image
            if (post.bookImageUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(post.bookImageUrl)
                    .placeholder(R.drawable.ic_book_placeholder)
                    .into(binding.bookCoverImage)
            } else {
                binding.bookCoverImage.setImageResource(R.drawable.ic_book_placeholder)
            }
            
            // Bind review content
            binding.reviewTitle.text = post.title
            binding.reviewText.text = post.review
            
            // Hide post image in list view as requested
            binding.postImage.visibility = View.GONE
            
            // Set like button state - add loading check
            binding.likeProgressBar.visibility = View.GONE
            binding.btnLike.isEnabled = true
            
            val isLiked = postViewModel?.isPostLikedByUser(post, currentUserId ?: "") ?: false
            binding.btnLike.isSelected = isLiked
            binding.btnLike.setIconResource(if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like_outline)
            
            // Format like count text
            val likeCount = post.likes
            binding.btnLike.text = when {
                likeCount == 0 -> "Like"
                likeCount == 1 -> "1 Like"
                else -> "$likeCount Likes"
            }

            // Check if the book is already in the user's wishlist
            checkIfInWishlist(post.bookId)
        }

        // Show loading state for this specific post
        fun showLikeLoading(postId: String, isLoading: Boolean) {
            // Only update if this is the correct post
            val position = adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val currentPostId = getItem(position).post.id
                if (currentPostId == postId) {
                    setLikeLoading(isLoading)
                }
            }
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            val date = Date(timestamp)
            val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return format.format(date)
        }

        private fun addToWishlist(bookId: String) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                // Log the bookId we're adding to wishlist for debugging
                Log.d("PostAdapter", "Adding book to wishlist: $bookId")
                
                // Before adding to Firebase, verify it's not empty or malformed
                if (bookId.isBlank()) {
                    Snackbar.make(binding.root, "Invalid book ID", Snackbar.LENGTH_SHORT).show()
                    return
                }
                
                val userRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
                userRef.update("wishlistBooks", FieldValue.arrayUnion(bookId))
                    .addOnSuccessListener {
                        binding.btnAddToList.text = "Already in Wishlist"
                        binding.btnAddToList.isEnabled = false
                        Snackbar.make(binding.root, "Book added to wishlist", Snackbar.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("PostAdapter", "Failed to add to wishlist: ${e.message}", e)
                        Snackbar.make(binding.root, "Failed to add to wishlist: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
            } else {
                Snackbar.make(binding.root, "You need to be logged in to add to wishlist", Snackbar.LENGTH_LONG).show()
            }
        }

        private fun checkIfInWishlist(bookId: String) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val userRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.uid)
                userRef.get()
                    .addOnSuccessListener { document ->
                        val wishlistBooks = document.get("wishlistBooks") as? List<String> ?: emptyList()
                        if (wishlistBooks.contains(bookId)) {
                            binding.btnAddToList.text = "Already in Wishlist"
                            binding.btnAddToList.isEnabled = false
                        } else {
                            binding.btnAddToList.text = "Add to Wishlist"
                            binding.btnAddToList.isEnabled = true
                        }
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(binding.root, "Failed to check wishlist: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<PostWithUser>() {
        override fun areItemsTheSame(oldItem: PostWithUser, newItem: PostWithUser): Boolean {
            return oldItem.post.id == newItem.post.id
        }

        override fun areContentsTheSame(oldItem: PostWithUser, newItem: PostWithUser): Boolean {
            return oldItem == newItem
        }
    }
}

// Data class to combine a post with its user information
data class PostWithUser(val post: Post, val user: User?)
