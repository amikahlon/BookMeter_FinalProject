package com.example.bookmeter.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.example.bookmeter.model.User

class AuthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

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
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun logout(onComplete: (Boolean, String?) -> Unit) {
        try {
            auth.signOut()
            _user.value = null
            onComplete(true, null)
        } catch (e: Exception) {
            onComplete(false, e.message)
        }
    }

    private fun fetchUserData(uid: String) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userModel = document.toObject(User::class.java)
                    _user.postValue(userModel)
                } else {
                    _user.postValue(null)
                }
            }
            .addOnFailureListener {
                _user.postValue(null)
            }
    }
}