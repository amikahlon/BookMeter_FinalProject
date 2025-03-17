package com.example.bookmeter.room

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertUser(user: UserEntity)

    @Query("SELECT * FROM user_table LIMIT 1")
    fun getUser(): LiveData<UserEntity?>

    @Query("SELECT * FROM user_table")
    fun getAllUsers(): List<UserEntity>

    @Delete
    fun deleteUser(user: UserEntity)

    @Delete
    fun deleteUsers(users: List<UserEntity>)
}
