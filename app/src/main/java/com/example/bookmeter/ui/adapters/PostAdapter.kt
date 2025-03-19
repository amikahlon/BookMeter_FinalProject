package com.example.bookmeter.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
    private val onMenuClick: (Post, View) -> Unit,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onPostClick: (Post) -> Unit
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
            binding.postOptionsMenu.setOnClickListener {
                onMenuClick(getItem(adapterPosition).post, it)
            }
            
            binding.btnLike.setOnClickListener {
                onLikeClick(getItem(adapterPosition).post)
            }
            
            binding.btnComment.setOnClickListener {
                onCommentClick(getItem(adapterPosition).post)
            }
            
            binding.root.setOnClickListener {
                onPostClick(getItem(adapterPosition).post)
            }
        }

        fun bind(postWithUser: PostWithUser) {
            val post = postWithUser.post
            val user = postWithUser.user
            
            // Bind user info - safely handle null user or missing properties
            binding.userName.text = user?.name ?: "Unknown User"
            binding.postTimestamp.text = formatTimestamp(post.timestamp)
            
            // Load user profile image - safely handle null or empty image URL
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
            binding.ratingBar.rating = post.rating.toFloat()
            binding.reviewText.text = post.review
            
            // Handle post image - new code to display post image
            if (post.imageUrl.isNotEmpty()) {
                binding.postImage.visibility = View.VISIBLE
                
                Glide.with(binding.root.context)
                    .load(post.imageUrl)
                    .into(binding.postImage)
                    
                // Add click listener to view image fullscreen
                binding.postImage.setOnClickListener {
                    // Could open fullscreen image viewer
                }
            } else {
                binding.postImage.visibility = View.GONE
            }
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
