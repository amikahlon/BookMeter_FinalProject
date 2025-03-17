package com.example.bookmeter.utils

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID

object StorageHelper {
    private const val TAG = "StorageHelper"
    private val storage = FirebaseStorage.getInstance()
    private val profileImagesRef = storage.reference.child("profile_images")
    
    /**
     * Uploads an image to Firebase Storage and returns the download URL
     */
    suspend fun uploadProfileImage(imageUri: Uri, userId: String): String? = withContext(Dispatchers.IO) {
        try {
            // Create a reference to the user's profile picture with unique filename
            val fileRef = profileImagesRef.child("$userId-${UUID.randomUUID()}.jpg")
            
            // Upload the file
            val uploadTask = fileRef.putFile(imageUri).await()
            
            // Get download URL
            val downloadUrl = fileRef.downloadUrl.await().toString()
            Log.d(TAG, "Image uploaded successfully: $downloadUrl")
            return@withContext downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading image", e)
            return@withContext null
        }
    }
}
