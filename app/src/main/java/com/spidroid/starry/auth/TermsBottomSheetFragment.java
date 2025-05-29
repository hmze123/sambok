package com.spidroid.starry.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.spidroid.starry.R;

public class TermsBottomSheetFragment extends BottomSheetDialogFragment {

  private static final String ARG_TERMS_TYPE = "terms_type";
  private FirebaseFirestore db = FirebaseFirestore.getInstance();

  public static TermsBottomSheetFragment newInstance(String termsType) {
    Bundle args = new Bundle();
    args.putString(ARG_TERMS_TYPE, termsType);
    TermsBottomSheetFragment fragment = new TermsBottomSheetFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.terms_bottom_sheet, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    TextView termsTitle = view.findViewById(R.id.termsTitle);
    TextView termsContent = view.findViewById(R.id.termsContent);
    View loadingProgress = view.findViewById(R.id.loadingProgress);

    String termsType = getArguments().getString(ARG_TERMS_TYPE);
    String collectionPath = "terms_and_conditions";
    String documentId = termsType.equals("terms") ? "terms_of_service" : "privacy_policy";

    termsTitle.setText(termsType.equals("terms") ? "Terms of Service" : "Privacy Policy");

    DocumentReference docRef = db.collection(collectionPath).document(documentId);
    docRef
        .get()
        .addOnCompleteListener(
            task -> {
              loadingProgress.setVisibility(View.GONE);

              if (task.isSuccessful() && task.getResult().exists()) {
                String content = task.getResult().getString("content");
                termsContent.setText(content);
                termsContent.setVisibility(View.VISIBLE);
              } else {
                Toast.makeText(getContext(), "Failed to load content", Toast.LENGTH_SHORT).show();
                dismiss();
              }
            });
  }
}
