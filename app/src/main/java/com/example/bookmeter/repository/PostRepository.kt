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
            val snapshot = postsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)
            }
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
}
