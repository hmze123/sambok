package com.spidroid.starry.utils

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

object TranslationHelper {

    private const val TAG = "TranslationHelper"
    private val translators = mutableMapOf<String, Translator>()

    // دالة للترجمة تستقبل النص، اللغة المصدر، اللغة الهدف، ومستمع للنتائج
    fun translate(
        text: String,
        sourceLang: String,
        targetLang: String,
        onResult: (Result<String>) -> Unit
    ) {
        val translatorKey = "$sourceLang-$targetLang"

        // الحصول على المترجم من الذاكرة المؤقتة أو إنشاء واحد جديد
        val translator = translators[translatorKey] ?: run {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLang) ?: TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLang) ?: TranslateLanguage.ENGLISH)
                .build()
            Translation.getClient(options).also {
                translators[translatorKey] = it
            }
        }

        // بناء الشروط اللازمة لتحميل نموذج اللغة (يتطلب اتصال واي فاي)
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()

        // تحميل النموذج إذا لم يكن موجودًا
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                Log.d(TAG, "Language model downloaded or already available.")
                // عند نجاح التحميل، قم بالترجمة
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        onResult(Result.success(translatedText))
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Translation failed.", exception)
                        onResult(Result.failure(exception))
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Model download failed.", exception)
                onResult(Result.failure(exception))
            }
    }

    // دالة لإغلاق جميع المترجمين عند عدم الحاجة إليهم (مثل عند إغلاق التطبيق)
    fun closeAllTranslators() {
        translators.values.forEach { it.close() }
        translators.clear()
        Log.d(TAG, "All translators closed.")
    }
}