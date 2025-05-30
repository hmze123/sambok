package com.spidroid.starry.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.spidroid.starry.models.CommentModel;
import com.spidroid.starry.repositories.CommentRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class CommentViewModel extends ViewModel {

    private final CommentRepository repository = new CommentRepository();
    private final MutableLiveData<List<CommentModel>> visibleComments = new MutableLiveData<>();
    private List<CommentModel> allComments = new ArrayList<>();
    private final Map<String, List<CommentModel>> replyMap = new HashMap<>();
    private final Set<String> expandedComments = new HashSet<>();

    public LiveData<List<CommentModel>> getVisibleComments() {
        return visibleComments;
    }

    public void loadComments(String postId) {
        repository.getCommentsForPost(postId).addOnSuccessListener(queryDocumentSnapshots -> {
            allComments = queryDocumentSnapshots.toObjects(CommentModel.class);
            // تعيين الـ ID لكل تعليق
            for (int i = 0; i < allComments.size(); i++) {
                allComments.get(i).setCommentId(queryDocumentSnapshots.getDocuments().get(i).getId());
            }
            buildReplyMapAndVisibleList();
        });
    }

    private void buildReplyMapAndVisibleList() {
        replyMap.clear();
        List<CommentModel> topLevelComments = new ArrayList<>();

        for (CommentModel comment : allComments) {
            comment.setDepth(0); // إعادة تعيين العمق
            if (comment.isTopLevel()) {
                topLevelComments.add(comment);
            } else {
                if (!replyMap.containsKey(comment.getParentCommentId())) {
                    replyMap.put(comment.getParentCommentId(), new ArrayList<>());
                }
                replyMap.get(comment.getParentCommentId()).add(comment);
            }
        }

        // حساب عمق الردود
        for (List<CommentModel> replies : replyMap.values()) {
            for (CommentModel reply : replies) {
                calculateDepth(reply, 1);
            }
        }

        rebuildVisibleList();
    }

    private void calculateDepth(CommentModel comment, int depth) {
        comment.setDepth(depth);
        if (replyMap.containsKey(comment.getCommentId())) {
            for (CommentModel reply : replyMap.get(comment.getCommentId())) {
                calculateDepth(reply, depth + 1);
            }
        }
    }

    public void toggleReplies(CommentModel parentComment) {
        if (expandedComments.contains(parentComment.getCommentId())) {
            expandedComments.remove(parentComment.getCommentId());
        } else {
            expandedComments.add(parentComment.getCommentId());
        }
        rebuildVisibleList();
    }

    private void rebuildVisibleList() {
        List<CommentModel> newVisibleList = new ArrayList<>();
        List<CommentModel> topLevelComments = allComments.stream()
                .filter(CommentModel::isTopLevel)
                .collect(Collectors.toList());

        for (CommentModel comment : topLevelComments) {
            newVisibleList.add(comment);
            if (expandedComments.contains(comment.getCommentId())) {
                addRepliesRecursively(comment, newVisibleList);
            }
        }
        visibleComments.setValue(newVisibleList);
    }
// ... (داخل كلاس CommentViewModel)

    public void addComment(String postId, Map<String, Object> commentData) {
        repository.addComment(postId, commentData).addOnSuccessListener(documentReference -> {
            // بعد إضافة التعليق بنجاح، نحدث عدد الردود في المنشور
            repository.updatePostReplyCount(postId, 1);
            // نعيد تحميل التعليقات لتظهر الإضافة الجديدة
            loadComments(postId);
        }).addOnFailureListener(e -> {
            // يمكنك هنا إرسال خطأ إلى الواجهة لعرضه
        });
    }

    public void toggleLike(String postId, CommentModel comment, String userId) {
        boolean isNowLiked = !comment.isLiked();
        comment.setLiked(isNowLiked);
        comment.setLikeCount(comment.getLikeCount() + (isNowLiked ? 1 : -1));
        visibleComments.setValue(new ArrayList<>(visibleComments.getValue())); // لتحديث الواجهة فوراً

        repository.toggleCommentLike(postId, comment.getCommentId(), userId, isNowLiked);
    }

    public void deleteComment(String postId, CommentModel comment) {
        repository.deleteComment(postId, comment.getCommentId()).addOnSuccessListener(aVoid -> {
            repository.updatePostReplyCount(postId, -1);
            // إعادة تحميل التعليقات بعد الحذف
            loadComments(postId);
        });
    }
    private void addRepliesRecursively(CommentModel parent, List<CommentModel> list) {
        List<CommentModel> replies = replyMap.get(parent.getCommentId());
        if (replies != null) {
            list.addAll(replies);
            for (CommentModel reply : replies) {
                if (expandedComments.contains(reply.getCommentId())) {
                    addRepliesRecursively(reply, list);
                }
            }
        }
    }
}