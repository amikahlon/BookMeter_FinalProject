package com.example.bookmeter.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.bookmeter.repository.UserRepository
import com.example.bookmeter.room.AppDatabase
import com.example.bookmeter.room.UserEntity
import kotlinx.coroutines.launch

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UserRepository
    val user: LiveData<UserEntity?>

    init {
        val userDao = AppDatabase.getDatabase(application).userDao()
        repository = UserRepository(userDao)
        user = repository.user
    }

    fun saveUser(uid: String, name: String) {
        viewModelScope.launch {
            repository.insertUser(UserEntity(uid, name))
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            repository.deleteUser(user)
        }
    }

    fun deleteAllUsers() {
        viewModelScope.launch {
            repository.clearAllUsers()
        }
    }
}