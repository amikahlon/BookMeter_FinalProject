package com.example.bookmeter.viewmodels

import android.app.Application
import androidx.lifecycle.*
import com.example.bookmeter.model.User
import com.example.bookmeter.repository.UserRepository
import com.example.bookmeter.room.AppDatabase
import com.example.bookmeter.room.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val userRepository: UserRepository
    val localUser: LiveData<UserEntity?>

    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        _currentUser.postValue(user)
        user?.let { fetchUserData(it.uid) }
    }

    init {
        val userDao = AppDatabase.getDatabase(getApplication()).userDao()
        userRepository = UserRepository(userDao)
        localUser = userRepository.user
        auth.addAuthStateListener(authListener)
        _currentUser.value = auth.currentUser
        auth.currentUser?.let { fetchUserData(it.uid) }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authListener)
    }

    fun registerUser(name: String, email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        _isLoading.value = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val userModel = User(uid = firebaseUser.uid, name = name, email = email)
                        db.collection("users").document(firebaseUser.uid)
                            .set(userModel)
                            .addOnSuccessListener {
                                _user.postValue(userModel)
                                saveUserToRoom(firebaseUser.uid, name)
                                _isLoading.postValue(false)
                                onComplete(true, null)
                            }
                            .addOnFailureListener { e ->
                                _isLoading.postValue(false)
                                onComplete(false, e.message)
                            }
                    } else {
                        _isLoading.postValue(false)
                        onComplete(false, "Failed to get current user after registration")
                    }
                } else {
                    _isLoading.postValue(false)
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun loginUser(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        _isLoading.value = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _isLoading.postValue(false)
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    firebaseUser?.let { fetchUserData(it.uid) }
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun logout(onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                auth.signOut()
                userRepository.clearAllUsers()
                _user.postValue(null)
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message)
            }
        }
    }

    private fun fetchUserData(uid: String) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userModel = document.toObject(User::class.java)
                    _user.postValue(userModel)
                    userModel?.let { saveUserToRoom(it.uid, it.name) }
                } else {
                    _user.postValue(null)
                }
            }
            .addOnFailureListener {
                _user.postValue(null)
            }
    }

    private fun saveUserToRoom(uid: String, name: String) {
        viewModelScope.launch {
            userRepository.insertUser(UserEntity(uid, name))
        }
    }
}
