package com.spidroid.starry.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.spidroid.starry.models.CommentModel
import com.spidroid.starry.repositories.CommentRepository
import java.util.stream.Collectors

class CommentViewModel : ViewModel() {
    private val repository = CommentRepository()
    private val visibleComments = MutableLiveData<MutableList<CommentModel?>?>()
    private var allComments: MutableList<CommentModel> = ArrayList<CommentModel>()
    private val replyMap: MutableMap<String?, MutableList<CommentModel>> =
        HashMap<String?, MutableList<CommentModel>>()
    private val expandedComments: MutableSet<String?> = HashSet<String?>()

    fun getVisibleComments(): LiveData<MutableList<CommentModel?>?> {
        return visibleComments
    }

    fun loadComments(postId: String?) {
        repository.getCommentsForPost(postId)
            .addOnSuccessListener(OnSuccessListener { queryDocumentSnapshots: QuerySnapshot? ->
                allComments = queryDocumentSnapshots.toObjects(CommentModel::class.java)
                // تعيين الـ ID لكل تعليق
                for (i in allComments.indices) {
                    allComments.get(i)
                        .setCommentId(queryDocumentSnapshots.getDocuments().get(i).getId())
                }
                buildReplyMapAndVisibleList()
            })
    }

    private fun buildReplyMapAndVisibleList() {
        replyMap.clear()
        val topLevelComments: MutableList<CommentModel?> = ArrayList<CommentModel?>()

        for (comment in allComments) {
            comment.setDepth(0) // إعادة تعيين العمق
            if (comment.isTopLevel()) {
                topLevelComments.add(comment)
            } else {
                if (!replyMap.containsKey(comment.getParentCommentId())) {
                    replyMap.put(comment.getParentCommentId(), ArrayList<CommentModel?>())
                }
                replyMap.get(comment.getParentCommentId())!!.add(comment)
            }
        }

        // حساب عمق الردود
        for (replies in replyMap.values) {
            for (reply in replies) {
                calculateDepth(reply, 1)
            }
        }

        rebuildVisibleList()
    }

    private fun calculateDepth(comment: CommentModel, depth: Int) {
        comment.setDepth(depth)
        if (replyMap.containsKey(comment.getCommentId())) {
            for (reply in replyMap.get(comment.getCommentId())!!) {
                calculateDepth(reply, depth + 1)
            }
        }
    }

    fun toggleReplies(parentComment: CommentModel) {
        if (expandedComments.contains(parentComment.getCommentId())) {
            expandedComments.remove(parentComment.getCommentId())
        } else {
            expandedComments.add(parentComment.getCommentId())
        }
        rebuildVisibleList()
    }

    private fun rebuildVisibleList() {
        val newVisibleList: MutableList<CommentModel?> = ArrayList<CommentModel?>()
        val topLevelComments = allComments.stream()
            .filter { obj: CommentModel? -> obj!!.isTopLevel() }
            .collect(Collectors.toList())

        for (comment in topLevelComments) {
            newVisibleList.add(comment)
            if (expandedComments.contains(comment.getCommentId())) {
                addRepliesRecursively(comment, newVisibleList)
            }
        }
        visibleComments.setValue(newVisibleList)
    }

    // ... (داخل كلاس CommentViewModel)
    fun addComment(postId: String?, commentData: MutableMap<String?, Any?>?) {
        repository.addComment(postId, commentData)
            .addOnSuccessListener(OnSuccessListener { documentReference: DocumentReference? ->
                // بعد إضافة التعليق بنجاح، نحدث عدد الردود في المنشور
                repository.updatePostReplyCount(postId, 1)
                // نعيد تحميل التعليقات لتظهر الإضافة الجديدة
                loadComments(postId)
            }).addOnFailureListener(OnFailureListener { e: Exception? -> })
    }

    fun toggleLike(postId: String?, comment: CommentModel, userId: String?) {
        val isNowLiked = !comment.isLiked()
        comment.setLiked(isNowLiked)
        comment.setLikeCount(comment.getLikeCount() + (if (isNowLiked) 1 else -1))
        visibleComments.setValue(ArrayList<CommentModel?>(visibleComments.getValue())) // لتحديث الواجهة فوراً

        repository.toggleCommentLike(postId, comment.getCommentId(), userId, isNowLiked)
    }

    fun deleteComment(postId: String?, comment: CommentModel) {
        repository.deleteComment(postId, comment.getCommentId())
            .addOnSuccessListener(OnSuccessListener { aVoid: Void? ->
                repository.updatePostReplyCount(postId, -1)
                // إعادة تحميل التعليقات بعد الحذف
                loadComments(postId)
            })
    }

    private fun addRepliesRecursively(parent: CommentModel, list: MutableList<CommentModel?>) {
        val replies = replyMap.get(parent.getCommentId())
        if (replies != null) {
            list.addAll(replies)
            for (reply in replies) {
                if (expandedComments.contains(reply.getCommentId())) {
                    addRepliesRecursively(reply, list)
                }
            }
        }
    }
}