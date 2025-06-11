package com.spidroid.starry.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.spidroid.starry.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TermsBottomSheetFragment : BottomSheetDialogFragment() {

    // A lazy-initialized property for the Firestore instance.
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment.
        return inflater.inflate(R.layout.terms_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find views by their ID.
        val termsTitle = view.findViewById<TextView>(R.id.termsTitle)
        val termsContent = view.findViewById<TextView>(R.id.termsContent)
        val loadingProgress = view.findViewById<View>(R.id.loadingProgress)

        // Get the type of terms to display from the arguments.
        val termsType = arguments?.getString(ARG_TERMS_TYPE) ?: run {
            // Dismiss the fragment if the type is not provided.
            Toast.makeText(context, "Error: Content type not specified.", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        // Determine Firestore path and title based on the terms type.
        val collectionPath = "terms_and_conditions"
        val (documentId, title) = when (termsType) {
            "terms" -> "terms_of_service" to "Terms of Service"
            else -> "privacy_policy" to "Privacy Policy"
        }

        termsTitle.text = title

        // Fetch the document from Firestore.
        db.collection(collectionPath).document(documentId)
            .get()
            .addOnCompleteListener { task ->
                // Ensure the fragment's view is still available before updating the UI.
                if (!isAdded) return@addOnCompleteListener

                loadingProgress.visibility = View.GONE

                if (task.isSuccessful && task.result?.exists() == true) {
                    // If successful, get the content and display it.
                    val content = task.result?.getString("content")
                    termsContent.text = content
                    termsContent.visibility = View.VISIBLE
                } else {
                    // If failed, show a toast and dismiss the fragment.
                    Toast.makeText(context, "Failed to load content", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
    }

    companion object {
        private const val ARG_TERMS_TYPE = "terms_type"

        /**
         * Creates a new instance of this fragment with the specified terms type.
         * @param termsType The type of content to load ("terms" or "privacy").
         * @return A new instance of TermsBottomSheetFragment.
         */
        @JvmStatic
        fun newInstance(termsType: String) = TermsBottomSheetFragment().apply {
            // Use bundleOf KTX for cleaner argument creation.
            arguments = bundleOf(ARG_TERMS_TYPE to termsType)
        }
    }
}
