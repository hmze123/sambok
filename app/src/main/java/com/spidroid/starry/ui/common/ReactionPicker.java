package com.spidroid.starry.ui.common; // أو الحزمة التي اخترتها

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.spidroid.starry.R;
import com.spidroid.starry.adapters.PostInteractionListener;
import com.spidroid.starry.models.PostModel;

public class ReactionPicker extends BottomSheetDialogFragment {

    private static final String ARG_POST = "post_model_for_reaction";
    private PostModel post;
    private PostInteractionListener interactionListener;

    // قائمة الإيموجيات المقترحة - يمكنك تخصيصها
    private final String[] EMOJIS = {"❤️", "😂", "😮", "😢", "👍", "👎", "🔥", "🎉"};

    public static ReactionPicker newInstance(PostModel post) {
        ReactionPicker fragment = new ReactionPicker();
        Bundle args = new Bundle();
        args.putParcelable(ARG_POST, post);
        fragment.setArguments(args);
        return fragment;
    }

    // دالة لتعيين الـ listener من الـ Fragment/Activity المستدعي
    public void setPostInteractionListener(PostInteractionListener listener) {
        this.interactionListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            post = getArguments().getParcelable(ARG_POST);
        }
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

        if (post == null || interactionListener == null) {
            dismiss(); // أغلق إذا لم يتم تمرير البيانات اللازمة
            return;
        }

        GridLayout reactionGrid = view.findViewById(R.id.reaction_grid);
        TextView title = view.findViewById(R.id.title);

        if (title != null) {
            // تأكد من إضافة هذا المورد إلى strings.xml
            // <string name="add_reaction_title">Add Reaction</string>
            title.setText(getString(R.string.add_reaction_title));
        }

        // إزالة أي إيموجيات قديمة إذا تم إعادة استخدام الـ view (على الرغم من أن BottomSheet عادةً ما يُنشئ view جديد)
        reactionGrid.removeAllViews();

        for (String emoji : EMOJIS) {
            // نفخ (inflate) item_emoji_reaction.xml لكل إيموجي
            TextView emojiView = (TextView) LayoutInflater.from(getContext())
                    .inflate(R.layout.item_emoji_reaction, reactionGrid, false);
            emojiView.setText(emoji);
            emojiView.setOnClickListener(v -> {
                interactionListener.onEmojiSelected(post, emoji); // استدعاء دالة الـ listener
                dismiss(); // إغلاق الـ BottomSheet بعد الاختيار
            });
            reactionGrid.addView(emojiView);
        }
    }
}