package com.example.bookmeter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bookmeter.databinding.FragmentWishListBinding
import com.example.bookmeter.viewmodels.AuthViewModel
import com.example.bookmeter.ui.adapters.WishlistAdapter
import com.google.firebase.firestore.FirebaseFirestore

class WishListFragment : Fragment() {
    private var _binding: FragmentWishListBinding? = null
    private val binding get() = _binding!!
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var wishlistAdapter: WishlistAdapter
    private val db = FirebaseFirestore.getInstance()

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
    }

    override fun onResume() {
        super.onResume()
        fetchWishlistFromFirebase()
    }

    private fun setupRecyclerView() {
        wishlistAdapter = WishlistAdapter()
        binding.rvWishlist.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = wishlistAdapter
        }
    }

    private fun observeWishlist() {
        authViewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                wishlistAdapter.submitList(it.wishlistBooks)
            }
        }
    }

    private fun fetchWishlistFromFirebase() {
        val currentUser = authViewModel.currentUser.value
        currentUser?.let { user ->
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    val wishlistBooks = document.get("wishlistBooks") as? List<String> ?: emptyList()
                    wishlistAdapter.submitList(wishlistBooks)
                }
                .addOnFailureListener { e ->
                    // Handle the error
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
