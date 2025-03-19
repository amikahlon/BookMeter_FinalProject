package com.example.bookmeter.repository

import com.example.bookmeter.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PostRepository {
    private val db = FirebaseFirestore.getInstance()
    private val postsCollection = db.collection("posts")
    
    suspend fun createPost(post: Post): Result<Post> {
        return try {
            // If ID is empty, let Firestore generate one
            val documentRef = if (post.id.isEmpty()) {
                postsCollection.document()
            } else {
                postsCollection.document(post.id)
            }
            
            // Create a copy of the post with the generated ID if needed
            val postToSave = if (post.id.isEmpty()) {
                post.copy(id = documentRef.id)
            } else {
                post
            }
            
            // Save to Firestore
            documentRef.set(postToSave).await()
            Result.success(postToSave)
        } catch (e: Exception) {
            Result.failure(e)
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
            val querySnapshot = postsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                
            val posts = querySnapshot.toObjects(Post::class.java)
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            postsCollection.document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
