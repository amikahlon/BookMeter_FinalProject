package com.example.bookmeter.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.bookmeter.R
import com.example.bookmeter.databinding.ItemBookBinding
import com.example.bookmeter.model.Book
import com.google.android.material.card.MaterialCardView

class BookAdapter(private val onBookSelected: (Book) -> Unit) :
    RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private var books: List<Book> = emptyList()
    private var lastPosition = -1

    fun submitList(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
        lastPosition = -1  // Reset for new search results
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.bind(book)
        
        // Add staggered animation for items
        setAnimation(holder.itemView, position)
    }
    
    private fun setAnimation(view: View, position: Int) {
        // If the position is greater than the last animated position,
        // animate the item
        if (position > lastPosition) {
            val animation = AnimationUtils.loadAnimation(
                view.context,
                R.anim.item_animation_fall_down
            )
            animation.startOffset = (position * 100).toLong()
            view.startAnimation(animation)
            lastPosition = position
        }
    }

    override fun getItemCount(): Int = books.size

    inner class BookViewHolder(private val binding: ItemBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.bookTitle.text = book.title
            binding.bookAuthor.text = book.authors.joinToString(", ")

            // Load book image with transition
            if (book.thumbnail.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(book.thumbnail)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerCrop()
                    .error(R.drawable.ic_book_placeholder)  // Use a placeholder image
                    .into(binding.bookImage)
                binding.bookImage.visibility = View.VISIBLE
            } else {
                // If no thumbnail, show a placeholder
                Glide.with(binding.root.context)
                    .load(R.drawable.ic_book_placeholder)
                    .into(binding.bookImage)
            }

            // Add touch feedback for selection
            binding.root.setOnClickListener {
                // Visual feedback on click
                (binding.root as MaterialCardView).apply {
                    animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction {
                            animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .withEndAction {
                                    // Select the book only after animation completes
                                    onBookSelected(book)
                                }
                        }
                        .start()
                }
                
                // Haptic feedback
                binding.root.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
    }
}
