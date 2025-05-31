package com.spidroid.starry.ui.notifications;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // ★ استيراد Log
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar; // ★ استيراد ProgressBar
import android.widget.TextView; // ★ استيراد TextView
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // ★ استيراد Nullable
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager; // ★ استيراد LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView; // ★ استيراد RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout; // ★ استيراد SwipeRefreshLayout

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.tabs.TabLayoutMediator; // ★ استيراد TabLayoutMediator (إذا كنت لا تزال تستخدمه)
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.spidroid.starry.R;
import com.spidroid.starry.activities.PostDetailActivity; // ★ استيراد PostDetailActivity
import com.spidroid.starry.activities.ProfileActivity;
import com.spidroid.starry.activities.SettingsActivity; // ★ استيراد SettingsActivity
import com.spidroid.starry.databinding.FragmentNotificationsBinding;
import com.spidroid.starry.models.NotificationModel; // ★ استيراد NotificationModel
import com.spidroid.starry.models.PostModel;
import com.spidroid.starry.models.UserModel;

import java.util.ArrayList; // ★ استيراد ArrayList

import de.hdodenhof.circleimageview.CircleImageView;

public class NotificationsFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

  private FragmentNotificationsBinding binding;
  private FirebaseAuth auth;
  private ListenerRegistration userListener;
  private NotificationsViewModel notificationsViewModel; // ★ إضافة ViewModel
  private NotificationAdapter notificationAdapter; // ★ إضافة Adapter
  // ★ لم نعد بحاجة لـ ViewPager و TabLayout هنا إذا كانت هذه الشاشة فقط للإشعارات
  // private ViewPager2 viewPager;
  // private TabLayout tabLayout;
  private RecyclerView notificationsRecyclerView; // ★ إضافة RecyclerView للإشعارات
  private ProgressBar loadingIndicator; // ★ إضافة ProgressBar
  private TextView emptyNotificationsTextView; // ★ إضافة TextView لحالة الفراغ
  private SwipeRefreshLayout swipeRefreshLayout; // ★ إضافة SwipeRefreshLayout


  @Override
  public View onCreateView(
          @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // استخدام ViewModelProvider للحصول على مثيل من NotificationsViewModel
    notificationsViewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

    binding = FragmentNotificationsBinding.inflate(inflater, container, false);
    View root = binding.getRoot();
    auth = FirebaseAuth.getInstance();

    // تهيئة عناصر واجهة المستخدم الجديدة
    notificationsRecyclerView = root.findViewById(R.id.recycler_view_notifications); // ستحتاج لإضافة هذا ID في XML
    loadingIndicator = root.findViewById(R.id.progress_bar_notifications); // ستحتاج لإضافة هذا ID في XML
    emptyNotificationsTextView = root.findViewById(R.id.text_view_empty_notifications); // ستحتاج لإضافة هذا ID في XML
    swipeRefreshLayout = root.findViewById(R.id.swipe_refresh_notifications); // ستحتاج لإضافة هذا ID في XML


    // إعداد RecyclerView
    setupRecyclerView();
    setupSwipeToRefresh(); // إعداد السحب للتحديث

    return root;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    // لم نعد بحاجة لإعداد ViewPager هنا إذا كانت الشاشة مخصصة فقط للإشعارات
    // setupViewPager();
    setupToolbarAvatarClick(); // نقل إعداد النقر على الأفاتار هنا
    observeViewModel();
  }

  @Override
  public void onStart() {
    super.onStart();
    // نقل setupToolbar إلى onStart أو onViewCreated إذا لم يكن يعتمد على بيانات متغيرة كثيرًا
    // لكن بما أنه يعتمد على بيانات المستخدم، قد يكون من الأفضل تحديثه عند توفر البيانات
    // سنقوم بتحديث الأفاتار من خلال مراقب بيانات المستخدم في UserModel إذا كان لديك
    loadCurrentUserData(); // جلب بيانات المستخدم لتحديث الأفاتار
  }
  private void loadCurrentUserData() {
    FirebaseUser currentUser = auth.getCurrentUser();
    if (currentUser != null && binding != null) {
      if (userListener != null) userListener.remove();
      userListener = FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
              .addSnapshotListener((documentSnapshot, error) -> {
                if (binding == null) return; // تحقق من أن binding ليس null (قد يكون Fragment تم تدميره)

                if (error != null) {
                  Log.w(TAG, "Listen failed for user data.", error);
                  binding.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar);
                  return;
                }
                if (documentSnapshot != null && documentSnapshot.exists()) {
                  UserModel user = documentSnapshot.toObject(UserModel.class);
                  if (user != null && user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty() && getContext() != null) {
                    Glide.with(getContext())
                            .load(user.getProfileImageUrl())
                            .placeholder(R.drawable.ic_default_avatar)
                            .error(R.drawable.ic_default_avatar)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(binding.ivUserAvatar);
                  } else {
                    binding.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar);
                  }
                } else {
                  binding.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar);
                }
              });
    } else if (binding != null) {
      binding.ivUserAvatar.setImageResource(R.drawable.ic_default_avatar);
    }
  }


  private void setupToolbarAvatarClick() {
    // إعداد النقر على صورة المستخدم في الشريط العلوي
    if (binding != null) { // التأكد من أن binding ليس null
      binding.ivUserAvatar.setOnClickListener(
              v -> {
                FirebaseUser currentUser = auth.getCurrentUser();
                if (currentUser != null) {
                  Intent profileIntent = new Intent(getActivity(), ProfileActivity.class);
                  profileIntent.putExtra("userId", currentUser.getUid());
                  startActivity(profileIntent);
                } else {
                  Toast.makeText(getContext(), "Please login to view profile", Toast.LENGTH_SHORT).show();
                }
              });
      // إعداد زر الإعدادات
      binding.ivSettings.setOnClickListener(v -> {
        Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
        startActivity(settingsIntent);
      });
    }
  }

  private void setupRecyclerView() {
    if (getContext() == null || notificationsRecyclerView == null) return; // ★ حماية إضافية

    notificationAdapter = new NotificationAdapter(getContext(), this);
    notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    notificationsRecyclerView.setAdapter(notificationAdapter);
  }

  private void setupSwipeToRefresh() {
    if (swipeRefreshLayout == null) return; // ★ حماية إضافية

    swipeRefreshLayout.setOnRefreshListener(() -> {
      Log.d(TAG, "Swipe to refresh triggered.");
      notificationsViewModel.fetchNotifications(); // إعادة جلب الإشعارات
    });
  }


  private void observeViewModel() {
    notificationsViewModel.getNotificationsList().observe(getViewLifecycleOwner(), notifications -> {
      if (notificationAdapter != null) { // ★ حماية إضافية
        notificationAdapter.submitList(notifications);
        if (emptyNotificationsTextView != null) { // ★ حماية إضافية
          emptyNotificationsTextView.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
        }
        Log.d(TAG, "Notifications list updated in Fragment: " + notifications.size());
      }
    });

    notificationsViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
      if (loadingIndicator != null) { // ★ حماية إضافية
        // لا نظهر مؤشر التحميل الرئيسي إذا كان السحب للتحديث نشطًا
        if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
          loadingIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
      }
      if (swipeRefreshLayout != null && !isLoading) { // ★ حماية إضافية
        swipeRefreshLayout.setRefreshing(false); // إيقاف مؤشر السحب للتحديث عند انتهاء التحميل
      }
      Log.d(TAG, "Loading state changed: " + isLoading);
    });

    notificationsViewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
      if (errorMsg != null && !errorMsg.isEmpty() && getContext() != null) { // ★ حماية إضافية
        Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
        Log.e(TAG, "Error observed: " + errorMsg);
      }
    });
  }

  // تطبيق واجهة OnNotificationClickListener
  @Override
  public void onNotificationClick(NotificationModel notification) {
    if (getContext() == null || notification == null) return; // ★ حماية إضافية

    // تحديث حالة الإشعار إلى مقروء (اختياري، يمكنك القيام به هنا أو عند فتح الشاشة)
    if (!notification.isRead()) {
      notificationsViewModel.markNotificationAsRead(notification.getNotificationId());
    }

    // الانتقال إلى الوجهة المناسبة بناءً على نوع الإشعار
    if (NotificationModel.TYPE_LIKE.equals(notification.getType()) || NotificationModel.TYPE_COMMENT.equals(notification.getType())) {
      if (notification.getPostId() != null && !notification.getPostId().isEmpty()) {
        // ملاحظة: PostDetailActivity يتوقع كائن PostModel كاملاً.
        // هنا لدينا فقط postId. ستحتاج إلى تعديل هذا الجزء إما:
        // 1. لجلب PostModel كاملاً بناءً على postId قبل الانتقال.
        // 2. أو تعديل PostDetailActivity لقبول postId فقط وجلب البيانات هناك.
        // للتبسيط الآن، سنفترض أن PostDetailActivity يمكنه التعامل مع postId فقط (وهو ليس صحيحًا حاليًا بناءً على الكود).
        // ★★ هذا الجزء يحتاج إلى مراجعة وتعديل ليتناسب مع كيفية عمل PostDetailActivity ★★
        // Intent intent = new Intent(getActivity(), PostDetailActivity.class);
        // intent.putExtra("postId", notification.getPostId()); // هذا مثال، قد تحتاج لتمرير PostModel
        // startActivity(intent);
        Toast.makeText(getContext(), "Clicked on notification for post: " + notification.getPostId(), Toast.LENGTH_SHORT).show();
        // ★★★ لتشغيل هذا بشكل صحيح، ستحتاج لجلب بيانات المنشور أولاً ★★★
        FirebaseFirestore.getInstance().collection("posts").document(notification.getPostId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                  if (documentSnapshot.exists()) {
                    PostModel post = documentSnapshot.toObject(PostModel.class);
                    if (post != null) {
                      post.setPostId(documentSnapshot.getId()); // التأكد من تعيين ID المنشور
                      Intent intent = new Intent(getActivity(), PostDetailActivity.class);
                      intent.putExtra(PostDetailActivity.EXTRA_POST, post);
                      startActivity(intent);
                    } else {
                      Toast.makeText(getContext(), "Failed to load post details.", Toast.LENGTH_SHORT).show();
                    }
                  } else {
                    Toast.makeText(getContext(), "Post not found.", Toast.LENGTH_SHORT).show();
                  }
                })
                .addOnFailureListener(e -> {
                  Toast.makeText(getContext(), "Error loading post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });


      } else {
        Toast.makeText(getContext(), "Post ID is missing for this notification.", Toast.LENGTH_SHORT).show();
      }
    } else if (NotificationModel.TYPE_FOLLOW.equals(notification.getType())) {
      if (notification.getFromUserId() != null && !notification.getFromUserId().isEmpty()) {
        Intent intent = new Intent(getActivity(), ProfileActivity.class);
        intent.putExtra("userId", notification.getFromUserId());
        startActivity(intent);
      } else {
        Toast.makeText(getContext(), "User ID is missing for follow notification.", Toast.LENGTH_SHORT).show();
      }
    } else {
      Toast.makeText(getContext(), "Notification type: " + notification.getType(), Toast.LENGTH_SHORT).show();
    }
  }


  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (userListener != null) {
      userListener.remove(); // Clean up listener
      userListener = null;
    }
    // لا تقم بإلغاء تسجيل notificationListener هنا، لأن ViewModel هو الذي يدير دورة حياته
    // notificationListener يتم إلغاء تسجيله في onCleared() داخل ViewModel
    binding = null; // ★ مهم جدًا لتجنب تسرب الذاكرة
    notificationsRecyclerView = null; // ★
    loadingIndicator = null; // ★
    emptyNotificationsTextView = null; // ★
    swipeRefreshLayout = null; // ★
    notificationAdapter = null; // ★

  }

  // لم نعد بحاجة لهذه الدالة هنا، فكل قسم له Fragment خاص به
  // private static class ViewPagerAdapter extends FragmentStateAdapter { /* ... */ }
}