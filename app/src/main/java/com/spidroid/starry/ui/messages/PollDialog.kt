package com.spidroid.starry.ui.messages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.spidroid.starry.R
import com.spidroid.starry.models.ChatMessage
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PollDialog : BottomSheetDialogFragment() {

    // واجهة لإعادة بيانات الاستطلاع إلى الـ Activity
    interface OnPollCreatedListener {
        fun onPollCreated(poll: ChatMessage.Poll)
    }

    private var listener: OnPollCreatedListener? = null
    private lateinit var optionsContainer: LinearLayout
    private lateinit var pollQuestionEditText: TextInputEditText
    private val optionEditTexts = mutableListOf<TextInputEditText>()

    fun setOnPollCreatedListener(listener: OnPollCreatedListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_poll, container, false)
        setupViews(view)
        return view
    }

    private fun setupViews(view: View) {
        optionsContainer = view.findViewById(R.id.options_container)
        pollQuestionEditText = view.findViewById(R.id.poll_question)

        // إضافة حقلي الخيارات الأوليين إلى القائمة
        optionEditTexts.add(view.findViewById(R.id.option_1))
        optionEditTexts.add(view.findViewById(R.id.option_2))

        val addOptionButton: Button = view.findViewById(R.id.add_option)
        val btnCancel: Button = view.findViewById(R.id.btn_cancel)
        val btnCreate: Button = view.findViewById(R.id.btn_create)

        addOptionButton.setOnClickListener { addNewOptionField() }
        btnCancel.setOnClickListener { dismiss() }
        btnCreate.setOnClickListener { createPoll() }
    }

    private fun addNewOptionField() {
        if (optionEditTexts.size >= 4) { // الحد الأقصى 4 خيارات
            Toast.makeText(context, "Maximum of 4 options allowed.", Toast.LENGTH_SHORT).show()
            return
        }

        // إنشاء حقل إدخال جديد ديناميكيًا
        val newOptionLayout = LayoutInflater.from(context).inflate(R.layout.item_poll_option_input, optionsContainer, false) as TextInputLayout
        val newOptionEditText = newOptionLayout.findViewById<TextInputEditText>(R.id.poll_option_input)
        newOptionLayout.hint = "Option ${optionEditTexts.size + 1}"

        optionsContainer.addView(newOptionLayout)
        optionEditTexts.add(newOptionEditText)
    }

    private fun createPoll() {
        val question = pollQuestionEditText.text.toString().trim()
        if (question.isBlank()) {
            pollQuestionEditText.error = "Question cannot be empty"
            return
        }

        val options = optionEditTexts
            .map { it.text.toString().trim() }
            .filter { it.isNotBlank() }

        if (options.size < 2) {
            Toast.makeText(context, "At least 2 options are required.", Toast.LENGTH_SHORT).show()
            return
        }

        val pollOptions = options.map { ChatMessage.PollOption(text = it) }
        val poll = ChatMessage.Poll(question = question, options = pollOptions.toMutableList())

        listener?.onPollCreated(poll)
        dismiss()
    }
}