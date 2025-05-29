package com.spidroid.starry.ui.messages;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.spidroid.starry.R;
import java.util.ArrayList;
import java.util.List;

public class PollDialog extends BottomSheetDialogFragment {

  public interface OnPollCreatedListener {
    void onPollCreated(String question, List<String> options);
  }

  private OnPollCreatedListener listener;
  private LinearLayout optionsContainer;
  private int optionCount = 2;

  public void setOnPollCreatedListener(OnPollCreatedListener listener) {
    this.listener = listener;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.dialog_poll, container, false);
    setupPollCreation(view);
    return view;
  }

  private void setupPollCreation(View view) {
    optionsContainer = view.findViewById(R.id.options_container);
    EditText pollQuestion = view.findViewById(R.id.poll_question);
    Button addOption = view.findViewById(R.id.add_option);
    Button btnCancel = view.findViewById(R.id.btn_cancel);
    Button btnCreate = view.findViewById(R.id.btn_create);

    addOption.setOnClickListener(v -> addNewOptionField());
    btnCancel.setOnClickListener(v -> dismiss());
    btnCreate.setOnClickListener(v -> createPoll(pollQuestion));
  }

  private void addNewOptionField() {
    if (optionCount >= 6) return; // Limit to 6 options

    TextInputLayout inputLayout = new TextInputLayout(requireContext());
    inputLayout.setLayoutParams(
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    inputLayout.setHint("Option " + (optionCount + 1));

    TextInputEditText editText = new TextInputEditText(inputLayout.getContext());
    editText.setLayoutParams(
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    inputLayout.addView(editText);

    optionsContainer.addView(inputLayout);
    optionCount++;
  }

  private void createPoll(EditText questionEditText) {
    String question = questionEditText.getText().toString().trim();
    List<String> options = new ArrayList<>();

    // Collect options from all input fields
    for (int i = 0; i < optionsContainer.getChildCount(); i++) {
      TextInputLayout inputLayout = (TextInputLayout) optionsContainer.getChildAt(i);
      EditText editText = (EditText) inputLayout.getChildAt(0);
      String option = editText.getText().toString().trim();
      if (!option.isEmpty()) {
        options.add(option);
      }
    }

    if (question.isEmpty()) {
      questionEditText.setError("Please enter a question");
      return;
    }

    if (options.size() < 2) {
      Toast.makeText(requireContext(), "At least 2 options required", Toast.LENGTH_SHORT).show();
      return;
    }

    if (listener != null) {
      listener.onPollCreated(question, options);
    }
    dismiss();
  }
}
