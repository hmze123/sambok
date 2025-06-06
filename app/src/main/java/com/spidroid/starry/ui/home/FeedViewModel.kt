// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/ui/home/FeedViewModel.kt
package com.spidroid.starry.ui.home

import androidx.lifecycle.LiveData // ✨ تم التأكد من هذا الاستيراد
import androidx.lifecycle.MutableLiveData // ✨ تم التأكد من هذا الاستيراد
import androidx.lifecycle.ViewModel // ✨ تم التأكد من هذا الاستيراد
import com.google.firebase.firestore.FirebaseFirestore
import com.spidroid.starry.models.PostModel // ✨ تم التأكد من هذا الاستيراد

class FeedViewModel : ViewModel() {
    private val posts: MutableLiveData<MutableList<PostModel?>?> =
        MutableLiveData<MutableList<PostModel?>?>()
    private val errors: MutableLiveData<String?> = MutableLiveData<String?>()
    private var hasLoaded = false

    fun hasPosts(): Boolean {
        // ✨ تم استخدام .value?.isEmpty()?.not() لتبسيط التحقق من القائمة غير الفارغة
        return posts.value != null && posts.value?.isEmpty()?.not() == true // أو !posts.value.isNullOrEmpty()
    }

    fun setPosts(newPosts: MutableList<PostModel?>?) {
        hasLoaded = true
        posts.value = newPosts
    }

    fun appendPosts(newPosts: MutableList<PostModel?>) {
        val current: MutableList<PostModel?> =
            if (posts.value != null) ArrayList<PostModel?>(posts.value) else ArrayList<PostModel?>()
        current.addAll(newPosts)
        posts.postValue(current)
    }

    fun toggleLike(postId: String?) {
        // Implement like toggle logic
    }

    fun toggleRepost(postId: String?) {
        // Implement repost toggle logic
    }

    fun toggleBookmark(postId: String?) {
        // Implement bookmark toggle logic
    }

    fun reportPost(postId: String?) {
        // Implement report logic
    }

    fun setError(error: String?) {
        errors.postValue(error)
    }

    // Getters
    fun getPosts(): LiveData<MutableList<PostModel?>?> {
        return posts
    }

    fun getErrors(): LiveData<String?> {
        return errors
    }

    fun deletePost(postId: String?) {
        if (postId == null) { // ✨ إضافة تحقق من عدم كون postId null
            errors.postValue("Post ID cannot be null for deletion.")
            return
        }
        FirebaseFirestore.getInstance()
            .collection("posts")
            .document(postId) // ✨ تم تمرير postId مباشرة (تم التحقق من عدم كونه null)
            .delete()
            .addOnSuccessListener(
                { aVoid ->
                    val current: MutableList<PostModel?>? = posts.value
                    if (current != null) {
                        current.removeIf { post: PostModel? -> post?.postId == postId } // ✨ تم تغيير post.getPostId() إلى post?.postId
                        posts.postValue(current)
                    }
                })
            .addOnFailureListener({ e -> errors.postValue("Failed to delete post: " + e.message) }) // ✨ تم تغيير e.getMessage() إلى e.message
    }

    fun resetState() {
        hasLoaded = false
        posts.value = null
    }
}