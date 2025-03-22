package com.example.bookmeter.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookmeter.R
import com.example.bookmeter.databinding.FragmentWishListBinding
import com.example.bookmeter.viewmodels.AuthViewModel
import com.example.bookmeter.ui.adapters.WishlistAdapter
import com.example.bookmeter.utils.SnackbarHelper
import com.google.firebase.firestore.FirebaseFirestore

class WishListFragment : Fragment(), WishlistAdapter.OnBookRemovedListener {
    private var _binding: FragmentWishListBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var wishlistAdapter: WishlistAdapter
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "WishListFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWishListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeWishlist()
        
        // Set initial loading state
        showLoading(true)
    }

    override fun onResume() {
        super.onResume()
        fetchWishlistFromFirebase()
    }

    private fun setupRecyclerView() {
        wishlistAdapter = WishlistAdapter()
        wishlistAdapter.setOnBookRemovedListener(this)
        binding.rvWishlist.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = wishlistAdapter
        }
    }

    private fun observeWishlist() {
        authViewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                if (it.wishlistBooks.isEmpty()) {
                    showEmptyState(true)
                } else {
                    showEmptyState(false)
                    wishlistAdapter.submitList(it.wishlistBooks)
                }
                showLoading(false)
            }
        }
    }

    private fun fetchWishlistFromFirebase() {
        showLoading(true)
        val currentUser = authViewModel.currentUser.value
        currentUser?.let { user ->
            Log.d(TAG, "Fetching wishlist for user: ${user.uid}")
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        Log.d(TAG, "Document data: ${document.data}")
                        val wishlistBooks = document.get("wishlistBooks") as? List<String> ?: emptyList()
                        Log.d(TAG, "Wishlist books: $wishlistBooks")
                        
                        if (wishlistBooks.isEmpty()) {
                            Log.d(TAG, "Wishlist is empty, showing empty state")
                            showEmptyState(true)
                        } else {
                            Log.d(TAG, "Found ${wishlistBooks.size} books in wishlist")
                            showEmptyState(false)
                            wishlistAdapter.submitList(wishlistBooks)
                        }
                    } else {
                        Log.d(TAG, "No document found for user ${user.uid}")
                        showEmptyState(true)
                    }
                    showLoading(false)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching wishlist", e)
                    showLoading(false)
                    showEmptyState(true)
                    SnackbarHelper.showError(binding.root, "Failed to load wishlist: ${e.message}")
                }
        } ?: run {
            Log.d(TAG, "No current user found")
            showLoading(false)
            showEmptyState(true)
            SnackbarHelper.showError(binding.root, "You need to be logged in to view your wishlist")
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        binding.loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    
    private fun showEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyStateLayout.visibility = View.VISIBLE
            binding.rvWishlist.visibility = View.GONE
        } else {
            binding.emptyStateLayout.visibility = View.GONE
            binding.rvWishlist.visibility = View.VISIBLE
        }
    }

    // Implement the OnBookRemovedListener interface
    override fun onBookRemoved(bookId: String) {
        Log.d(TAG, "Book removed: $bookId, refreshing wishlist")
        fetchWishlistFromFirebase()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
