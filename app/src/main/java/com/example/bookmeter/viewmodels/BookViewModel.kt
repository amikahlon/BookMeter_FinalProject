package com.example.bookmeter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bookmeter.model.Book
import com.example.bookmeter.repository.BookRepository
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class BookViewModel(private val repository: BookRepository) : ViewModel() {

    private val _books = MutableLiveData<List<Book>>()
    val books: LiveData<List<Book>> = _books
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    
    // To avoid multiple simultaneous searches
    private val isSearching = AtomicBoolean(false)
    
    // Debounce mechanism
    private var lastQuery = ""
    private var lastSearchTime = 0L

    fun searchBooks(query: String) {
        // Basic debouncing - don't search again if same query within 500ms
        val currentTime = System.currentTimeMillis()
        if (query == lastQuery && currentTime - lastSearchTime < 500) {
            return
        }
        
        lastQuery = query
        lastSearchTime = currentTime
        
        // If already searching, don't start a new search
        if (isSearching.getAndSet(true)) {
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.searchBooks(query)
                _books.postValue(result)
            } catch (e: Exception) {
                _errorMessage.postValue("Search failed: ${e.message}")
                _books.postValue(emptyList())
            } finally {
                _isLoading.value = false
                isSearching.set(false)
            }
        }
    }
}

/**
 * Factory מותאם אישית כדי להעביר את ה-Repository ל-ViewModel
 */
class BookViewModelFactory(private val repository: BookRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}