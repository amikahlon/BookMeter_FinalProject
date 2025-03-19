package com.example.bookmeter.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bookmeter.R
import com.example.bookmeter.databinding.ItemFormBookSearchBinding
import com.example.bookmeter.databinding.ItemFormHeaderBinding
import com.example.bookmeter.databinding.ItemFormImageUploadBinding
import com.example.bookmeter.databinding.ItemFormRatingBinding
import com.example.bookmeter.databinding.ItemFormReviewDetailsBinding

class FormAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_BOOK_SEARCH = 1
        const val TYPE_REVIEW_DETAILS = 2
        const val TYPE_RATING = 3
        const val TYPE_IMAGE_UPLOAD = 4  // New type for image upload
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> TYPE_HEADER
            1 -> TYPE_BOOK_SEARCH
            2 -> TYPE_REVIEW_DETAILS
            3 -> TYPE_RATING
            4 -> TYPE_IMAGE_UPLOAD  // Added new position
            else -> throw IllegalArgumentException("Invalid position $position")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding = ItemFormHeaderBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                HeaderViewHolder(binding)
            }
            TYPE_BOOK_SEARCH -> {
                val binding = ItemFormBookSearchBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                BookSearchViewHolder(binding)
            }
            TYPE_REVIEW_DETAILS -> {
                val binding = ItemFormReviewDetailsBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ReviewDetailsViewHolder(binding)
            }
            TYPE_RATING -> {
                val binding = ItemFormRatingBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                RatingViewHolder(binding)
            }
            TYPE_IMAGE_UPLOAD -> {
                val binding = ItemFormImageUploadBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ImageUploadViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // Binding will be handled by fragment directly
    }

    override fun getItemCount(): Int = 5  // Updated count to include image upload

    class HeaderViewHolder(val binding: ItemFormHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class BookSearchViewHolder(val binding: ItemFormBookSearchBinding) : RecyclerView.ViewHolder(binding.root)
    class ReviewDetailsViewHolder(val binding: ItemFormReviewDetailsBinding) : RecyclerView.ViewHolder(binding.root)
    class RatingViewHolder(val binding: ItemFormRatingBinding) : RecyclerView.ViewHolder(binding.root)
    class ImageUploadViewHolder(val binding: ItemFormImageUploadBinding) : RecyclerView.ViewHolder(binding.root)
}
