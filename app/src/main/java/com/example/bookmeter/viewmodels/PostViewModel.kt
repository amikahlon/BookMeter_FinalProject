package com.example.bookmeter.viewmodels

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmeter.model.Post
import com.example.bookmeter.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class PostViewModel : ViewModel() {
    private val repository = PostRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _newPostResult = MutableLiveData<Result<Post>?>()
    val newPostResult: LiveData<Result<Post>?> = _newPostResult
    
    private val _userPosts = MutableLiveData<Result<List<Post>>?>()
    val userPosts: LiveData<Result<List<Post>>?> = _userPosts
    
    fun createPost(
        bookId: String,
        bookName: String,
        bookImageUrl: String,
        title: String,
        review: String,
        rating: Int,
        imageUri: Uri? = null // Added parameter for post image
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _newPostResult.value = Result.failure(IllegalStateException("User not logged in"))
            return
        }
        
        _isLoading.value = true
        
        viewModelScope.launch {
            val post = Post(
                bookId = bookId,
                bookName = bookName,
                bookImageUrl = bookImageUrl,
                title = title,
                review = review,
                rating = rating,
                userId = currentUser.uid,
                timestamp = System.currentTimeMillis()
                // imageUrl will be set in repository after upload
            )
            
            val result = repository.createPost(post, imageUri)
            _newPostResult.postValue(result)
            _isLoading.postValue(false)
        }
    }
    
    fun resetNewPostResult() {
        _newPostResult.value = null
    }
    
    fun getUserPosts(userId: String? = null) {
        val targetUserId = userId ?: auth.currentUser?.uid
        if (targetUserId == null) {
            _userPosts.value = Result.failure(IllegalStateException("No user ID provided"))
            return
        }
        
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.getPostsByUser(targetUserId)
            _userPosts.postValue(result)
            _isLoading.postValue(false)
        }
    }
    
    fun deletePost(postId: String) {
        _isLoading.value = true
        repository.deletePost(postId)
            .addOnSuccessListener {
                // Refresh user posts after successful deletion
                auth.currentUser?.uid?.let { getUserPosts(it) }
            }
            .addOnFailureListener {
                // Set loading to false if deletion fails
                _isLoading.postValue(false)
            }
    }
}
