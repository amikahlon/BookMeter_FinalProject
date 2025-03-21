package com.example.bookmeter.repository

import android.net.Uri
import com.example.bookmeter.model.Post
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PostRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val postsCollection = firestore.collection("posts")
    
    suspend fun createPost(post: Post, imageUri: Uri? = null): Result<Post> {
        return try {
            // If there's an image to upload, do that first
            val imageUrl = imageUri?.let { uploadImage(it) } ?: ""
            
            // Create a post with the image URL
            val postWithImage = post.copy(imageUrl = imageUrl)
            
            // Generate ID and save to Firestore
            val id = postsCollection.document().id
            val finalPost = postWithImage.copy(id = id)
            
            postsCollection.document(id).set(finalPost).await()
            Result.success(finalPost)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadImage(imageUri: Uri): String {
        return try {
            val filename = "post_images/${UUID.randomUUID()}"
            val storageRef = storage.reference.child(filename)
            
            // Upload file and get download URL
            val uploadTask = storageRef.putFile(imageUri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw e
        }
    }
    
    suspend fun getPostById(postId: String): Result<Post?> {
        return try {
            val document = postsCollection.document(postId).get().await()
            if (document.exists()) {
                Result.success(document.toObject(Post::class.java))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPostsByUser(userId: String): Result<List<Post>> {
        return try {
            // Simpler query that doesn't require a composite index
            val snapshot = postsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                
            // We'll sort the results in memory instead of in the query
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)
            }.sortedByDescending { it.timestamp }
            
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a post and its associated image if it exists
     * @param postId ID of the post to delete
     * @return Task indicating success or failure
     */
    fun deletePost(postId: String): Task<Void> {
        val postRef = postsCollection.document(postId)
        
        // First get the post to check if it has an image to delete
        return postRef.get()
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Failed to fetch post")
                }
                
                val post = task.result.toObject(Post::class.java)
                
                // Delete the post document
                val deleteTask = postRef.delete()
                
                // If post had an image URL and it's not empty, delete from storage too
                if (post?.imageUrl?.isNotEmpty() == true) {
                    deleteTask.addOnSuccessListener {
                        try {
                            // Extract storage path from URL and delete the image
                            // Note: This runs independently of the post deletion
                            val storageRef = storage.getReferenceFromUrl(post.imageUrl)
                            storageRef.delete()
                                .addOnFailureListener { e ->
                                    // Just log the error but don't fail the whole operation
                                    android.util.Log.e("PostRepository", "Failed to delete image: ${e.message}")
                                }
                        } catch (e: Exception) {
                            // Log but don't fail if image deletion fails
                            android.util.Log.e("PostRepository", "Error parsing image URL: ${e.message}")
                        }
                    }
                }
                
                // Return the task for deleting the post document
                deleteTask
            }
    }

    /**
     * Toggle like status for a post
     * @param postId The post ID
     * @param userId The user ID
     * @return Task representing if the operation was successful
     */
    fun toggleLike(postId: String, userId: String): Task<Void> {
        return firestore.runTransaction { transaction ->
            val postRef = postsCollection.document(postId)
            val postSnapshot = transaction.get(postRef)
            
            if (!postSnapshot.exists()) {
                throw Exception("Post not found")
            }
            
            val post = postSnapshot.toObject(Post::class.java)
            if (post == null) {
                throw Exception("Failed to parse post data")
            }
            
            // Check if user already liked the post
            val userLiked = post.likedBy.contains(userId)
            
            if (userLiked) {
                // User already liked the post, so remove the like
                transaction.update(postRef, 
                    "likedBy", FieldValue.arrayRemove(userId),
                    "likes", post.likes - 1
                )
            } else {
                // User hasn't liked the post, so add the like
                transaction.update(postRef, 
                    "likedBy", FieldValue.arrayUnion(userId),
                    "likes", post.likes + 1
                )
            }
            
            null
        }
    }
    
    /**
     * Check if a user has liked a post
     * @param postId The post ID
     * @param userId The user ID
     * @return Task with Boolean result indicating if the user has liked the post
     */
    fun hasUserLikedPost(postId: String, userId: String): Task<Boolean> {
        return postsCollection.document(postId)
            .get()
            .continueWith { task ->
                if (!task.isSuccessful || task.result == null) {
                    return@continueWith false
                }
                
                val post = task.result.toObject(Post::class.java)
                post?.likedBy?.contains(userId) ?: false
            }
    }
    
    /**
     * Get the count of likes for a post
     * @param postId The post ID
     * @return Task with Integer result representing the like count
     */
    fun getLikeCount(postId: String): Task<Int> {
        return postsCollection.document(postId)
            .get()
            .continueWith { task ->
                if (!task.isSuccessful || task.result == null) {
                    return@continueWith 0
                }
                
                val post = task.result.toObject(Post::class.java)
                post?.likes ?: 0
            }
    }

    /**
     * Update an existing post
     * @param postId The ID of the post to update
     * @param updates Map of fields to update
     * @param newImageUri Optional new image to upload
     * @param shouldRemoveImage Flag to indicate if the image should be removed
     * @return Task representing the success or failure of the operation
     */
    fun updatePost(postId: String, updates: Map<String, Any>, newImageUri: Uri? = null, shouldRemoveImage: Boolean = false): Task<Void> {
        val postRef = postsCollection.document(postId)
        
        return postRef.get()
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    throw task.exception ?: Exception("Failed to fetch post")
                }
                
                val post = task.result.toObject(Post::class.java)
                    ?: throw Exception("Post not found")
                
                // Handle image removal explicitly
                if (shouldRemoveImage) {
                    // Create a new updates map with the imageUrl set to empty string
                    val updatedFields = HashMap(updates)
                    updatedFields["imageUrl"] = ""
                    
                    // Delete the old image if it exists
                    if (post.imageUrl.isNotEmpty()) {
                        try {
                            val oldImageRef = storage.getReferenceFromUrl(post.imageUrl)
                            oldImageRef.delete()
                                .addOnFailureListener { e ->
                                    android.util.Log.e("PostRepository", "Error deleting old image: ${e.message}")
                                }
                        } catch (e: Exception) {
                            android.util.Log.e("PostRepository", "Error getting storage reference: ${e.message}")
                        }
                    }
                    
                    return@continueWithTask postRef.update(updatedFields)
                }
                
                // If there's a new image, upload it first, then update the post
                if (newImageUri != null) {
                    // Upload the new image
                    val filename = "post_images/${UUID.randomUUID()}"
                    val storageRef = storage.reference.child(filename)
                    
                    return@continueWithTask storageRef.putFile(newImageUri)
                        .continueWithTask { uploadTask ->
                            if (!uploadTask.isSuccessful) {
                                throw uploadTask.exception ?: Exception("Failed to upload image")
                            }
                            
                            // Get the download URL
                            storageRef.downloadUrl
                        }
                        .continueWithTask { downloadUrlTask ->
                            if (!downloadUrlTask.isSuccessful) {
                                throw downloadUrlTask.exception ?: Exception("Failed to get download URL")
                            }
                            
                            // Update the post with the new image URL and other updates
                            val updatedFields = HashMap(updates)
                            updatedFields["imageUrl"] = downloadUrlTask.result.toString()
                            
                            // Delete the old image if it exists
                            val oldImageUrl = post.imageUrl
                            if (oldImageUrl.isNotEmpty()) {
                                try {
                                    val oldImageRef = storage.getReferenceFromUrl(oldImageUrl)
                                    oldImageRef.delete()
                                } catch (e: Exception) {
                                    android.util.Log.e("PostRepository", "Error deleting old image: ${e.message}")
                                }
                            }
                            
                            postRef.update(updatedFields)
                        }
                } else {
                    // No new image, just update the fields
                    return@continueWithTask postRef.update(updates)
                }
            }
    }
}
