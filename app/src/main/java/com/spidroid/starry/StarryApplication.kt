// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/StarryApplication.kt
package com.spidroid.starry

import android.app.Application
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import dagger.hilt.android.HiltAndroidApp

// يمكنك إزالة هذا السطر إذا كنت لا تستخدم BuildConfig في مكان آخر
// import com.spidroid.starry.BuildConfig
@HiltAndroidApp
class StarryApplication : Application() {

    private companion object {
        private const val TAG_APP_CHECK = "StarryAppCheck"
    }

    override fun onCreate() {
        super.onCreate()

        Glide.get(this).setMemoryCategory(MemoryCategory.HIGH)
        FirebaseApp.initializeApp(this)

        val firebaseAppCheck = FirebaseAppCheck.getInstance()

        // ✨ بداية التعديل: إزالة التعليق عن الشرط ✨
        // يتحقق تلقائياً إذا كان نوع البناء هو "debug"
        if (BuildConfig.DEBUG) {
            // في وضع التصحيح، استخدم موفر التصحيح
            // ستحتاج إلى إعداد debug token في Firebase Console
            firebaseAppCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG_APP_CHECK, "App Check initialized with Debug provider.")
        } else {
            // في وضع الإنتاج، استخدم Play Integrity
            firebaseAppCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
            Log.d(TAG_APP_CHECK, "App Check initialized with Play Integrity provider.")
        }
        // ✨ نهاية التعديل ✨

        val firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        firestore.firestoreSettings = settings
    }
}