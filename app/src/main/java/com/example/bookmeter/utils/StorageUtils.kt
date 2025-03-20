package com.example.bookmeter.utils

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID


object StorageUtils {
    private val storage = FirebaseStorage.getInstance()
    

    fun deleteImageFromUrl(imageUrl: String): Task<Void>? {
        return try {
            if (imageUrl.isEmpty()) {
                return null
            }
            
            val storageRef = storage.getReferenceFromUrl(imageUrl)
            storageRef.delete()
        } catch (e: Exception) {
            android.util.Log.e("StorageUtils", "Error deleting image: ${e.message}")
            null
        }
    }

    fun uploadImage(imageUri: Uri, path: String = "images"): Task<Uri> {
        val filename = "$path/${UUID.randomUUID()}"
        val storageRef = storage.reference.child(filename)
        
        return storageRef.putFile(imageUri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Unknown upload error")
                }
                storageRef.downloadUrl
            }
    }
}
