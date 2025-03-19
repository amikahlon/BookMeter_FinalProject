package com.example.bookmeter.repository

import android.net.Uri
import com.example.bookmeter.model.Post
import com.google.firebase.firestore.FirebaseFirestore
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
    
    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            // Get post to check if it has an image to delete
            val post = postsCollection.document(postId).get().await().toObject(Post::class.java)
            
            // Delete the post document
            postsCollection.document(postId).delete().await()
            
            // If post had an image URL, delete from storage too
            post?.imageUrl?.let { imageUrl ->
                if (imageUrl.isNotEmpty()) {
                    try {
                        // Extract storage path from URL
                        val storageRef = storage.getReferenceFromUrl(imageUrl)
                        storageRef.delete().await()
                    } catch (e: Exception) {
                        // Log but don't fail if image deletion fails
                        e.printStackTrace()
                    }
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
