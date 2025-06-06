package com.spidroid.starry.ui.home

import com.google.firebase.firestore.FirebaseFirestore

class FeedViewModel : ViewModel() {
    private val posts: MutableLiveData<kotlin.collections.MutableList<PostModel?>?> =
        MutableLiveData<kotlin.collections.MutableList<PostModel?>?>()
    private val errors: MutableLiveData<kotlin.String?> = MutableLiveData<kotlin.String?>()
    private var hasLoaded = false

    fun hasPosts(): kotlin.Boolean {
        return posts.getValue() != null && !posts.getValue().isEmpty()
    }

    fun setPosts(newPosts: kotlin.collections.MutableList<PostModel?>?) {
        hasLoaded = true
        posts.setValue(newPosts)
    }

    fun appendPosts(newPosts: kotlin.collections.MutableList<PostModel?>) {
        val current: kotlin.collections.MutableList<PostModel?> =
            if (posts.getValue() != null) java.util.ArrayList<PostModel?>(posts.getValue()) else java.util.ArrayList<PostModel?>()
        current.addAll(newPosts)
        posts.postValue(current)
    }

    fun toggleLike(postId: kotlin.String?) {
        // Implement like toggle logic
    }

    fun toggleRepost(postId: kotlin.String?) {
        // Implement repost toggle logic
    }

    fun toggleBookmark(postId: kotlin.String?) {
        // Implement bookmark toggle logic
    }

    fun reportPost(postId: kotlin.String?) {
        // Implement report logic
    }

    fun setError(error: kotlin.String?) {
        errors.postValue(error)
    }

    // Getters
    fun getPosts(): LiveData<kotlin.collections.MutableList<PostModel?>?> {
        return posts
    }

    fun getErrors(): LiveData<kotlin.String?> {
        return errors
    }

    fun deletePost(postId: kotlin.String?) {
        FirebaseFirestore.getInstance()
            .collection("posts")
            .document(postId)
            .delete()
            .addOnSuccessListener(
                { aVoid ->
                    val current: kotlin.collections.MutableList<PostModel?>? = posts.getValue()
                    if (current != null) {
                        current.removeIf { post: PostModel? -> post.getPostId() == postId }
                        posts.postValue(current)
                    }
                })
            .addOnFailureListener({ e -> errors.postValue("Failed to delete post: " + e.getMessage()) })
    }

    fun resetState() {
        hasLoaded = false
        posts.setValue(null)
    }
}
