package com.example.bookmeter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookmeter.model.Post
import com.example.bookmeter.repository.PostRepository
import kotlinx.coroutines.launch

class MyReviewsViewModel : ViewModel() {
    private val postRepository = PostRepository()
    
    private val _userPosts = MutableLiveData<Result<List<Post>>?>()
    val userPosts: LiveData<Result<List<Post>>?> = _userPosts
    
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    
    fun getUserPosts(userId: String) {
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val result = postRepository.getPostsByUser(userId)
                _userPosts.value = result
            } catch (e: Exception) {
                _userPosts.value = Result.failure(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshUserPosts(userId: String) {
        getUserPosts(userId)
    }
}
