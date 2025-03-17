package com.example.bookmeter.repository

import androidx.lifecycle.LiveData
import com.example.bookmeter.room.UserDao
import com.example.bookmeter.room.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val userDao: UserDao) {
    val user: LiveData<UserEntity?> = userDao.getUser()

    suspend fun insertUser(user: UserEntity) {
        withContext(Dispatchers.IO) {
            userDao.insertUser(user)
        }
    }

    suspend fun deleteUser(user: UserEntity) {
        withContext(Dispatchers.IO) {
            userDao.deleteUser(user)
        }
    }

    suspend fun clearAllUsers() {
        withContext(Dispatchers.IO) {
            val users = userDao.getAllUsers()
            if (users.isNotEmpty()) {
                userDao.deleteUsers(users)
            }
        }
    }
}
