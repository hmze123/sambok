package com.spidroid.starry.viewmodels;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;
import com.spidroid.starry.repositories.PostRepository;
import java.util.ArrayList;
import java.util.List;

public class PostViewModel extends ViewModel {

    private final PostRepository postRepository = new PostRepository();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    private final MutableLiveData<List<PostModel>> posts = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<List<UserModel>> suggestions = new MutableLiveData<>();
    private final MediatorLiveData<List<Object>> combinedFeed = new MediatorLiveData<>();

    public PostViewModel() {
        // ندمج مصادر البيانات (المنشورات واقتراحات المستخدمين) في LiveData واحد ليعرض في الـ RecyclerView.
        // عندما يتغير أي من هذه المصادر، سيتم استدعاء combineData.
        combinedFeed.addSource(posts, postsList -> combineData(postsList, suggestions.getValue()));
        combinedFeed.addSource(suggestions, users -> combineData(posts.getValue(), users));
    }

    public LiveData<List<Object>> getCombinedFeed() {
        return combinedFeed;
    }

    public LiveData<String> getErrorLiveData() {
        return error;
    }

    /**
     * تجلب المنشورات من Firestore وتحدث LiveData الخاص بالمنشورات.
     * تتضمن خطوات تشخيصية للتحقق من البيانات.
     * @param limit العدد الأقصى للمنشورات المراد جلبها.
     */
    public void fetchPosts(int limit) {
        postRepository.getPosts(limit).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<PostModel> postList = new ArrayList<>();
                for (DocumentSnapshot doc : task.getResult()) {

                    // *** خطوة التشخيص 1: طباعة البيانات الخام من مستند Firestore ***
                    // هذه الطباعة ستساعدنا في رؤية ما إذا كانت البيانات تصل بشكل صحيح من قاعدة البيانات.
                    Log.d("PostViewModel_Debug", "Firestore Document Data: " + doc.getData());

                    PostModel post = doc.toObject(PostModel.class);
                    if (post != null) {

                        // *** خطوة التشخيص 2: طباعة كائن PostModel بعد تحويله ***
                        // هذه الطباعة ستكشف ما إذا كان هناك مشكلة في تحويل البيانات إلى كائن PostModel.
                        Log.d("PostViewModel_Debug", "Converted Post Object, Content: " + post.getContent() + ", PostId: " + doc.getId());

                        // *** خطوة الإصلاح والتحقق: تجاهل المنشورات ذات المحتوى الفارغ أو المفقود ***
                        // في بعض الأحيان، قد تحتوي قاعدة البيانات على مستندات غير مكتملة.
                        // نتأكد من أن المحتوى موجود وغير فارغ قبل إضافته للقائمة.
                        if (post.getContent() != null && !post.getContent().isEmpty()) {
                            post.setPostId(doc.getId()); // تأكد من تعيين الـ ID من المستند
                            updateUserInteractions(post); // تحديث تفاعلات المستخدم (إعجابات، حفظ، إلخ)
                            postList.add(post);
                        } else {
                            Log.w("PostViewModel_Debug", "Skipping post with null or empty content. ID: " + doc.getId());
                        }
                    } else {
                        Log.w("PostViewModel_Debug", "Skipping null PostModel object. Document ID: " + doc.getId());
                    }
                }
                posts.postValue(postList); // تحديث LiveData بقائمة المنشورات الصالحة
            } else {
                // إذا فشلت المهمة، قم بتحديث LiveData للخطأ
                error.postValue("Failed to load posts: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                Log.e("PostViewModel_Debug", "Failed to fetch posts", task.getException());
            }
        });
    }

    /**
     * تجلب اقتراحات المستخدمين (مثلاً: المستخدمين الذين قد يعرفهم المستخدم الحالي).
     */
    public void fetchUserSuggestions() {
        postRepository.getUserSuggestions().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                suggestions.postValue(task.getResult().toObjects(UserModel.class));
            } else {
                Log.e("PostViewModel_Debug", "Failed to fetch user suggestions", task.getException());
            }
        });
    }

    /**
     * تبديل حالة الإعجاب بمنشور معين.
     * @param postId معرّف المنشور.
     * @param isLiked الحالة الجديدة للإعجاب (true إذا كان معجبًا، false إذا ألغى الإعجاب).
     */
    public void toggleLike(String postId, boolean isLiked) {
        if (auth.getUid() == null) {
            error.postValue("User not authenticated to like posts.");
            return;
        }
        postRepository.toggleLike(postId, isLiked)
                .addOnFailureListener(e -> error.postValue("Failed to toggle like: " + e.getMessage()));
    }

    /**
     * تبديل حالة حفظ المنشور.
     * @param postId معرّف المنشور.
     * @param isBookmarked الحالة الجديدة للحفظ.
     */
    public void toggleBookmark(String postId, boolean isBookmarked) {
        if (auth.getUid() == null) {
            error.postValue("User not authenticated to bookmark posts.");
            return;
        }
        postRepository.toggleBookmark(postId, isBookmarked)
                .addOnFailureListener(e -> error.postValue("Failed to toggle bookmark: " + e.getMessage()));
    }

    /**
     * تبديل حالة إعادة نشر المنشور.
     * @param postId معرّف المنشور.
     * @param isReposted الحالة الجديدة لإعادة النشر.
     */
    public void toggleRepost(String postId, boolean isReposted) {
        if (auth.getUid() == null) {
            error.postValue("User not authenticated to repost.");
            return;
        }
        postRepository.toggleRepost(postId, isReposted)
                .addOnFailureListener(e -> error.postValue("Failed to toggle repost: " + e.getMessage()));
    }

    /**
     * حذف منشور.
     * @param postId معرّف المنشور المراد حذفه.
     */
    public void deletePost(String postId) {
        postRepository.deletePost(postId)
                .addOnSuccessListener(aVoid -> {
                    // تحديث القائمة بعد الحذف بنجاح (يمكنك إعادة جلب المنشورات أو إزالة المنشور المحذوف من القائمة)
                    fetchPosts(15); // إعادة جلب المنشورات لتحديث القائمة
                })
                .addOnFailureListener(e -> error.postValue("Failed to delete post: " + e.getMessage()));
    }

    /**
     * تدمج قائمة المنشورات واقتراحات المستخدمين في قائمة واحدة ليتم عرضها في الـ RecyclerView.
     * يمكن تخصيص منطق الدمج هنا (مثلاً: عرض اقتراحات المستخدمين بعد 5 منشورات، ثم كل 10 منشورات، إلخ).
     * @param postsList قائمة المنشورات.
     * @param users قائمة المستخدمين المقترحين.
     */
    private void combineData(List<PostModel> postsList, List<UserModel> users) {
        List<Object> combinedList = new ArrayList<>();
        if (postsList != null) {
            combinedList.addAll(postsList);
        }

        // مثال بسيط: إضافة اقتراحات المستخدمين بعد أول 3 منشورات
        if (users != null && !users.isEmpty()) {
            if (combinedList.size() >= 3) {
                combinedList.add(3, users.get(0)); // إضافة أول اقتراح بعد المنشور الثالث
            } else if (!combinedList.isEmpty()) {
                combinedList.add(users.get(0)); // إذا كان هناك منشورات ولكن أقل من 3، أضف الاقتراح في النهاية
            }
        }
        combinedFeed.postValue(combinedList);
    }

    /**
     * تحدث حالة تفاعلات المستخدم (إعجابات، حفظ، إعادة نشر) للمنشور بناءً على
     * بيانات المستخدم الحالي في Firebase.
     * هذه الدالة ضرورية لتحديث واجهة المستخدم بشكل صحيح لكل منشور عند عرضه.
     * @param post كائن PostModel المراد تحديثه.
     */
    private void updateUserInteractions(PostModel post) {
        String currentUserId = auth.getUid();
        if (currentUserId == null) {
            // إذا لم يكن هناك مستخدم مسجل الدخول، فلا توجد تفاعلات
            post.setLiked(false);
            post.setBookmarked(false);
            post.setReposted(false);
            return;
        }

        // التحقق مما إذا كان المستخدم قد أعجب بالمنشور
        if (post.getLikes() != null && post.getLikes().containsKey(currentUserId)) {
            post.setLiked(true);
        } else {
            post.setLiked(false);
        }

        // التحقق مما إذا كان المستخدم قد حفظ المنشور
        if (post.getBookmarks() != null && post.getBookmarks().containsKey(currentUserId)) {
            post.setBookmarked(true);
        } else {
            post.setBookmarked(false);
        }

        // التحقق مما إذا كان المستخدم قد أعاد نشر المنشور
        if (post.getReposts() != null && post.getReposts().containsKey(currentUserId)) {
            post.setReposted(true);
        } else {
            post.setReposted(false);
        }
    }
}