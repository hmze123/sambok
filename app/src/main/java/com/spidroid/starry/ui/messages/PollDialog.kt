package com.spidroid.starry.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.spidroid.starry.R

class PollDialog : BottomSheetDialogFragment() {
    interface OnPollCreatedListener {
        fun onPollCreated(question: String?, options: MutableList<String?>?)
    }

    private var listener: OnPollCreatedListener? = null
    private var optionsContainer: LinearLayout? = null
    private var optionCount = 2

    fun setOnPollCreatedListener(listener: OnPollCreatedListener?) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_poll, container, false)
        setupPollCreation(view)
        return view
    }

    private fun setupPollCreation(view: View) {
        optionsContainer = view.findViewById<LinearLayout>(R.id.options_container)
        val pollQuestion = view.findViewById<EditText>(R.id.poll_question)
        val addOption = view.findViewById<Button>(R.id.add_option)
        val btnCancel = view.findViewById<Button>(R.id.btn_cancel)
        val btnCreate = view.findViewById<Button>(R.id.btn_create)

        addOption.setOnClickListener(View.OnClickListener { v: View? -> addNewOptionField() })
        btnCancel.setOnClickListener(View.OnClickListener { v: View? -> dismiss() })
        btnCreate.setOnClickListener(View.OnClickListener { v: View? -> createPoll(pollQuestion) })
    }

    private fun addNewOptionField() {
        if (optionCount >= 6) return  // Limit to 6 options


        val inputLayout = TextInputLayout(requireContext())
        inputLayout.setLayoutParams(
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        inputLayout.setHint("Option " + (optionCount + 1))

        val editText = TextInputEditText(inputLayout.getContext())
        editText.setLayoutParams(
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        inputLayout.addView(editText)

        optionsContainer!!.addView(inputLayout)
        optionCount++
    }

    private fun createPoll(questionEditText: EditText) {
        val question = questionEditText.getText().toString().trim { it <= ' ' }
        val options: MutableList<String?> = ArrayList<String?>()

        // Collect options from all input fields
        for (i in 0..<optionsContainer!!.getChildCount()) {
            val inputLayout = optionsContainer!!.getChildAt(i) as TextInputLayout
            val editText = inputLayout.getChildAt(0) as EditText
            val option = editText.getText().toString().trim { it <= ' ' }
            if (!option.isEmpty()) {
                options.add(option)
            }
        }

        if (question.isEmpty()) {
            questionEditText.setError("Please enter a question")
            return
        }

        if (options.size < 2) {
            Toast.makeText(requireContext(), "At least 2 options required", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (listener != null) {
            listener!!.onPollCreated(question, options)
        }
        dismiss()
    }
}
