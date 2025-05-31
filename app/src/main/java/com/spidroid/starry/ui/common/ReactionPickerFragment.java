package com.spidroid.starry.ui.common; // أو الحزمة المناسبة لمشروعك

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.spidroid.starry.R; // تأكد من أن R مستوردة بشكل صحيح

public class ReactionPickerFragment extends BottomSheetDialogFragment {

    public static final String TAG = "ReactionPickerFragment";

    // واجهة للاستماع إلى اختيار الريأكشن
    public interface ReactionListener {
        void onReactionSelected(String emojiUnicode);
    }

    private ReactionListener reactionListener;

    // دالة لتعيين الـ Listener من الـ Fragment أو الـ Activity المستدعي
    public void setReactionListener(ReactionListener listener) {
        this.reactionListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // استخدام ملف التخطيط reaction_picker.xml
        return inflater.inflate(R.layout.reaction_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        GridLayout reactionGrid = view.findViewById(R.id.reaction_grid);

        // يمكنك الحصول على قائمة الرموز التعبيرية من strings.xml أو تعريفها هنا
        // الترتيب مهم إذا كنت ستعتمد على الـ tags في reaction_picker.xml
        String[] emojis = {"❤️", "😂", "😮", "😢", "👍", "👎"}; // نفس الرموز الموجودة في reaction_picker.xml
        String[] emojiTags = {"heart", "laugh", "wow", "sad", "like", "dislike"}; // الـ tags المقابلة

        if (reactionGrid.getChildCount() == emojis.length) { // تأكد من أن عدد الـ TextViews يطابق عدد الـ emojis
            for (int i = 0; i < reactionGrid.getChildCount(); i++) {
                View child = reactionGrid.getChildAt(i);
                if (child instanceof TextView) {
                    TextView emojiView = (TextView) child;
                    final String selectedEmoji = emojiView.getText().toString(); // الحصول على الـ emoji مباشرة من النص

                    // يمكنك التحقق من الـ tag إذا أردت التأكد أكثر
                    // String tag = emojiView.getTag() != null ? emojiView.getTag().toString() : "";
                    // if (tag.equals(emojiTags[i])) { ... }

                    emojiView.setOnClickListener(v -> {
                        if (reactionListener != null) {
                            reactionListener.onReactionSelected(selectedEmoji);
                        }
                        dismiss(); // إغلاق الـ BottomSheet بعد الاختيار
                    });
                }
            }
        } else {
            // يمكنك إضافة Log هنا إذا كان عدد العناصر غير متطابق كتحذير
            android.util.Log.w(TAG, "Mismatch between defined emojis and TextViews in reaction_picker.xml");
        }
    }

    // (اختياري) يمكنك إضافة المزيد من التخصيصات هنا، مثل تغيير ارتفاع الـ BottomSheet
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        // يمكنك إضافة المزيد من الإعدادات للـ dialog هنا إذا أردت
        return dialog;
    }
}