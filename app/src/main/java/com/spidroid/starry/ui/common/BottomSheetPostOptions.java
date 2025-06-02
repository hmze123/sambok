package com.spidroid.starry.ui.common; // أو الحزمة الصحيحة لمشروعك

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // كمثال، قد تكون تستخدم MaterialButton
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.auth.FirebaseAuth; // ستحتاجه للتحقق من المستخدم الحالي
import com.spidroid.starry.R; // تأكد من استيراد R
import com.spidroid.starry.adapters.PostInteractionListener;
import com.spidroid.starry.models.PostModel;

public class BottomSheetPostOptions extends BottomSheetDialogFragment {

    public static final String TAG = "BottomSheetPostOptions";
    private static final String ARG_POST = "post_model";

    private PostModel post;
    private PostInteractionListener interactionListener;
    private String currentUserId;

    // دالة لإنشاء نسخة جديدة من الـ BottomSheet مع تمرير بيانات المنشور
    public static BottomSheetPostOptions newInstance(PostModel post) {
        BottomSheetPostOptions fragment = new BottomSheetPostOptions();
        Bundle args = new Bundle();
        args.putParcelable(ARG_POST, post);
        fragment.setArguments(args);
        return fragment;
    }

    // لتعيين الـ listener من الـ Activity أو الـ Fragment المستدعي
    public void setPostInteractionListener(PostInteractionListener listener) {
        this.interactionListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            post = getArguments().getParcelable(ARG_POST);
        }
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // استخدام ملف التخطيط bottom_sheet_post_options.xml
        View view = inflater.inflate(R.layout.bottom_sheet_post_options, container, false);
        // يمكنك إضافة المزيد من تخصيصات التصميم هنا
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (post == null || interactionListener == null) {
            dismiss(); // أغلق الـ BottomSheet إذا لم تكن البيانات أو الـ listener متاحة
            return;
        }

        boolean isAuthor = currentUserId != null && currentUserId.equals(post.getAuthorId());

        // الحصول على مراجع للأزرار من ملف التخطيط
        // افترض أن لديك هذه المعرفات في bottom_sheet_post_options.xml
        View optionPin = view.findViewById(R.id.option_pin_post_bs); // ستحتاج لإضافة هذا ID في XML
        View optionEdit = view.findViewById(R.id.option_edit_post_bs);
        View optionDelete = view.findViewById(R.id.option_delete_post_bs);
        View optionCopyLink = view.findViewById(R.id.option_copy_link_bs);
        View optionShare = view.findViewById(R.id.option_share_post_bs);
        View optionSave = view.findViewById(R.id.option_save_post_bs);
        View optionEditPrivacy = view.findViewById(R.id.option_edit_privacy_bs);
        View optionReport = view.findViewById(R.id.option_report_post_bs);
        // يمكنك استخدام MaterialButton إذا كانت لديك أيقونات ونصوص
        // مثال لزر الحفظ:
        Button saveButton = view.findViewById(R.id.option_save_post_bs); // افترض أن هذا هو ID زر الحفظ

        // التحكم في ظهور الخيارات
        if (optionPin != null) optionPin.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
        if (optionEdit != null) optionEdit.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
        if (optionDelete != null) optionDelete.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
        if (optionEditPrivacy != null) optionEditPrivacy.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
        if (optionReport != null) optionReport.setVisibility(isAuthor ? View.GONE : View.VISIBLE);

        // تحديث نص زر الحفظ
        if (saveButton != null) {
            saveButton.setText(post.isBookmarked() ? "إلغاء حفظ المنشور" : "حفظ المنشور");
        }


        // تعيين مستمعي النقر
        if (optionPin != null) {
            optionPin.setOnClickListener(v -> {
                interactionListener.onTogglePinPostClicked(post);
                dismiss();
            });
        }
        if (optionEdit != null) {
            optionEdit.setOnClickListener(v -> {
                interactionListener.onEditPost(post);
                dismiss();
            });
        }
        if (optionDelete != null) {
            optionDelete.setOnClickListener(v -> {
                interactionListener.onDeletePost(post);
                dismiss();
            });
        }
        if (optionCopyLink != null) {
            optionCopyLink.setOnClickListener(v -> {
                interactionListener.onCopyLink(post);
                dismiss();
            });
        }
        if (optionShare != null) {
            optionShare.setOnClickListener(v -> {
                interactionListener.onSharePost(post);
                dismiss();
            });
        }
        if (optionSave != null) {
            optionSave.setOnClickListener(v -> {
                // ★★★ هذا هو السطر الذي يسبب الخطأ، تم تعديله ★★★
                interactionListener.onBookmarkClicked(post);
                dismiss();
            });
        }
        if (optionEditPrivacy != null) {
            optionEditPrivacy.setOnClickListener(v -> {
                interactionListener.onEditPostPrivacy(post);
                dismiss();
            });
        }
        if (optionReport != null) {
            optionReport.setOnClickListener(v -> {
                interactionListener.onReportPost(post);
                dismiss();
            });
        }
    }
}