package com.example.bookmeter.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.example.bookmeter.model.User
import com.example.bookmeter.repository.UserRepository
import com.example.bookmeter.room.AppDatabase
import com.example.bookmeter.room.UserEntity
import com.example.bookmeter.utils.StorageHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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

    fun registerUser(
        name: String, 
        email: String, 
        password: String, 
        profileImageUri: Uri?,
        onComplete: (Boolean, String?) -> Unit
    ) {
        _isLoading.value = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        // Launch coroutine to handle the image upload if needed
                        viewModelScope.launch {
                            var profilePictureUrl = ""
                            
                            // Upload the profile image if provided
                            if (profileImageUri != null) {
                                profilePictureUrl = StorageHelper.uploadProfileImage(
                                    profileImageUri, 
                                    firebaseUser.uid
                                ) ?: ""
                            }
                            
                            // Create user model with all the new fields
                            val userModel = User(
                                uid = firebaseUser.uid,
                                name = name,
                                email = email,
                                profilePictureUrl = profilePictureUrl,
                                wishlistBooks = listOf(),  // Empty wishlist for new users
                                readBooks = listOf()       // Empty read books for new users
                            )
                            
                            // Save user to Firestore
                            db.collection("users").document(firebaseUser.uid)
                                .set(userModel)
                                .addOnSuccessListener {
                                    _user.postValue(userModel)
                                    saveUserToRoom(firebaseUser.uid, name, profilePictureUrl)
                                    _isLoading.postValue(false)
                                    onComplete(true, null)
                                }
                                .addOnFailureListener { e ->
                                    _isLoading.postValue(false)
                                    onComplete(false, e.message)
                                }
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
        _isLoading.value = true
        try {
            // First sign out from Firebase
            auth.signOut()
            
            // Then clear local data in a coroutine
            viewModelScope.launch {
                try {
                    // Clear user data from Room database
                    userRepository.clearAllUsers()
                    
                    // Update LiveData values on the main thread
                    _user.postValue(null)
                    _currentUser.postValue(null)
                    
                    // Only now report completion
                    _isLoading.postValue(false)
                    onComplete(true, null)
                } catch (e: Exception) {
                    _isLoading.postValue(false)
                    onComplete(false, e.message ?: "Error clearing local data")
                }
            }
        } catch (e: Exception) {
            _isLoading.postValue(false)
            onComplete(false, e.message ?: "Error during logout")
        }
    }

    private fun fetchUserData(uid: String) {
        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val userModel = document.toObject(User::class.java)
                    _user.postValue(userModel)
                    userModel?.let { 
                        saveUserToRoom(it.uid, it.name, it.profilePictureUrl)
                    }
                } else {
                    _user.postValue(null)
                }
            }
            .addOnFailureListener {
                _user.postValue(null)
            }
    }

    private fun saveUserToRoom(uid: String, name: String, profilePictureUrl: String = "") {
        viewModelScope.launch {
            userRepository.insertUser(UserEntity(uid, name, profilePictureUrl))
        }
    }

    /**
     * Update user profile information
     */
    fun updateUserProfile(
        name: String,
        imageUri: Uri?,
        callback: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    _isLoading.value = false
                    callback(false, "User not logged in")
                    return@launch
                }
                
                val userId = currentUser.uid
                
                // Get current user data from Firebase
                val db = FirebaseFirestore.getInstance()
                val currentUserDoc = db.collection("users").document(userId).get().await()
                val currentUserData = currentUserDoc.toObject(User::class.java) ?: User(uid = userId)
                
                // Process profile image if provided
                var profilePictureUrl = currentUserData.profilePictureUrl
                if (imageUri != null) {
                    // Upload new image
                    val newImageUrl = StorageHelper.uploadProfileImage(imageUri, userId)
                    if (newImageUrl != null) {
                        profilePictureUrl = newImageUrl
                        
                        // Update Firebase Auth profile picture
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setPhotoUri(Uri.parse(newImageUrl))
                            .build()
                        currentUser.updateProfile(profileUpdates).await()
                    }
                }
                
                // Update name in Firebase Auth
                val nameUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                currentUser.updateProfile(nameUpdates).await()
                
                // Create updated user object
                val updatedUser = User(
                    uid = userId,
                    name = name,
                    email = currentUserData.email,
                    profilePictureUrl = profilePictureUrl,
                    wishlistBooks = currentUserData.wishlistBooks,
                    readBooks = currentUserData.readBooks
                )
                
                // Update in Firestore
                db.collection("users").document(userId).set(updatedUser).await()
                
                // Update in Room
                val userEntity = UserEntity(
                    uid = userId,
                    name = name,
                    profilePictureUrl = profilePictureUrl
                )
                userRepository.insertUser(userEntity)
                
                // Refresh the user LiveData
                _user.postValue(updatedUser)
                
                _isLoading.value = false
                callback(true, "Profile updated successfully")
                
            } catch (e: Exception) {
                _isLoading.value = false
                callback(false, e.message ?: "Error updating profile")
            }
        }
    }

    /**
     * Force refresh user data from Firestore
     */
    fun refreshUserData() {
        auth.currentUser?.let { user ->
            fetchUserData(user.uid)
        }
    }
}
