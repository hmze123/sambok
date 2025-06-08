package com.spidroid.starry.utils

import android.app.Activity
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

/**
 * دالة مساعدة آمنة لعرض Snackbar من أي Activity.
 * تستخدم الـ View الجذر للـ Activity لضمان عدم حدوث خطأ.
 *
 * @param message الرسالة التي سيتم عرضها.
 */
fun Activity.showSnackbar(message: String) {
    val rootView = findViewById<View>(android.R.id.content)
    Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
}

/**
 * دالة مساعدة آمنة لعرض Snackbar من أي Fragment.
 * تستخدم requireView() للتأكد من أن واجهة الـ Fragment متاحة.
 *
 * @param message الرسالة التي سيتم عرضها.
 */
fun Fragment.showSnackbar(message: String) {
    // requireView() تضمن أن الـ view الخاص بالـ Fragment موجود,
    // وإلا فإنه يطلق استثناءً واضحًا مما يسهل اكتشاف الأخطاء.
    Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show()
}