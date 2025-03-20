package com.example.bookmeter.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bookmeter.R
import com.example.bookmeter.databinding.ItemPostBinding
import com.example.bookmeter.model.Post
import com.example.bookmeter.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostAdapter(
    private val onEditClick: (Post) -> Unit,
    private val onDeleteClick: (Post) -> Unit,
    private val onLikeClick: (Post) -> Unit,
    private val onPostClick: (Post) -> Unit,
    private val currentUserId: String? = null
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
                    onLikeClick(getItem(position).post)
                }
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
            } else {
                binding.ownerActionsContainer.visibility = View.GONE
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
        }
        
        private fun formatTimestamp(timestamp: Long): String {
            val date = Date(timestamp)
            val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            return format.format(date)
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
