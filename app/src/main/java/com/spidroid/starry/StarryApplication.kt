package com.spidroid.starry

import android.app.Application
import android.util.Log // ★ إضافة هذا إذا كنت تستخدم Log.d
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
// ★★ تأكد من وجود هذه الاستيرادات ★★
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory // أو SafetyNet إذا كنت تستخدمه
// ★★ نهاية التأكيد ★★
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
// import com.google.firebase.firestore.ktx.firestoreSettings // طريقة Kotlin KTX البديلة (اختيارية)

class StarryApplication : Application() {

    // يمكن تعريف TAG_APP_CHECK داخل companion object إذا أردت استخدامه كـ static field
    // أو مباشرة كـ private const val إذا كان استخدامه محصورًا داخل هذا الكلاس فقط
    private companion object {
        private const val TAG_APP_CHECK = "StarryAppCheck" // لتسجيلات AppCheck
    }
    // بديل: private const val TAG_APP_CHECK = "StarryAppCheck"

    override fun onCreate() {
        super.onCreate()

        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)
        FirebaseApp.initializeApp(this)

        // تهيئة Firebase App Check
        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        // استخدم DebugAppCheckProviderFactory للاختبار في وضع التصحيح
        // للحصول على debug token، راجع Logcat بعد تشغيل التطبيق لأول مرة مع هذا الكود
        // ستحتاج لإضافة هذا الـ token في إعدادات App Check في Firebase console
        // if (BuildConfig.DEBUG) { // BuildConfig.DEBUG يُتاح تلقائيًا
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        Log.d(TAG_APP_CHECK, "Using DebugAppCheckProviderFactory.")
        // } else {
        //     // استخدم PlayIntegrityAppCheckProviderFactory لإصدار الإنتاج
        //     firebaseAppCheck.installAppCheckProviderFactory(
        //         PlayIntegrityAppCheckProviderFactory.getInstance()
        //     )
        //     Log.d(TAG_APP_CHECK, "Using PlayIntegrityAppCheckProviderFactory.")
        // }
        // ملاحظة: لقد علقت جزء else مؤقتًا لتسهيل الاختبار مع debug provider دائمًا.
        // تذكر إعادته عند بناء نسخة الإنتاج.


        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        firestore.firestoreSettings = settings // استخدام property access syntax في Kotlin

        // أو باستخدام طريقة KTX (إذا أضفت اعتمادية firebase-firestore-ktx):
        // val settingsKtx = firestoreSettings {
        //     isPersistenceEnabled = true
        //     // يمكنك إضافة إعدادات أخرى هنا
        // }
        // firestore.firestoreSettings = settingsKtx
    }
}