package com.example.bookmeter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bookmeter.model.Post
import com.example.bookmeter.model.User
import com.example.bookmeter.ui.adapters.PostWithUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import timber.log.Timber

class PostListViewModel : ViewModel() {
    
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _posts = MutableLiveData<List<PostWithUser>>()
    val posts: LiveData<List<PostWithUser>> = _posts
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // Cache user data to avoid redundant queries
    private val userCache = mutableMapOf<String, User>()
    
    init {
        loadPosts()
    }
    
    fun loadPosts() {
        _isLoading.value = true
        
        firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    _posts.value = emptyList()
                    _isLoading.value = false
                    return@addOnSuccessListener
                }
                
                val posts = documents.toObjects(Post::class.java)
                val postsWithUsers = mutableListOf<PostWithUser>()
                var completedUserQueries = 0
                
                if (posts.isEmpty()) {
                    _posts.value = emptyList()
                    _isLoading.value = false
                    return@addOnSuccessListener
                }
                
                // Create temporary list with null users
                val tempList = posts.map { post ->
                    PostWithUser(post, null)
                }
                _posts.value = tempList
                
                // Fetch user data for each post
                posts.forEach { post ->
                    fetchUserForPost(post) { user ->
                        completedUserQueries++
                        
                        // Add to cache
                        if (user != null) {
                            userCache[post.userId] = user
                        }
                        
                        // Add to result list
                        postsWithUsers.add(PostWithUser(post, user))
                        
                        // When all user queries complete, update the LiveData
                        if (completedUserQueries == posts.size) {
                            // Sort by timestamp (newest first)
                            val sortedPosts = postsWithUsers.sortedByDescending { it.post.timestamp }
                            _posts.value = sortedPosts
                            _isLoading.value = false
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Error loading posts")
                _errorMessage.value = "Failed to load posts: ${e.message}"
                _isLoading.value = false
            }
    }
    
    private fun fetchUserForPost(post: Post, callback: (User?) -> Unit) {
        // Check cache first
        if (userCache.containsKey(post.userId)) {
            callback(userCache[post.userId])
            return
        }
        
        firestore.collection("users").document(post.userId)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                callback(user)
            }
            .addOnFailureListener { e ->
                Timber.e(e, "Error fetching user ${post.userId}")
                callback(null)
            }
    }
    
    fun refreshPosts() {
        loadPosts()
    }
}
