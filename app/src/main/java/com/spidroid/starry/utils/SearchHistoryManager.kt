package com.spidroid.starry.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SearchHistoryManager(context: Context) {
    private val sharedPreferences: SharedPreferences
    private val gson = Gson()

    init {
        sharedPreferences = context.getSharedPreferences(
            SearchHistoryManager.Companion.PREFS_NAME,
            Context.MODE_PRIVATE
        )
    }

    val searchHistory: MutableList<String?>
        // دالة لجلب قائمة البحث
        get() {
            val json =
                sharedPreferences.getString(SearchHistoryManager.Companion.KEY_SEARCH_HISTORY, null)
            if (json == null) {
                return ArrayList<String?>()
            }
            val type = object :
                TypeToken<ArrayList<String?>?>() {}.getType()
            return gson.fromJson<MutableList<String?>>(json, type)
        }

    // دالة لإضافة مصطلح بحث جديد
    fun addSearchTerm(term: String?) {
        if (term == null || term.trim { it <= ' ' }.isEmpty()) {
            return
        }
        val cleanedTerm = term.trim { it <= ' ' }
        val history = this.searchHistory

        // إزالة المصطلح إذا كان موجوداً بالفعل لإضافته في الأعلى
        history.remove(cleanedTerm)

        // إضافة المصطلح الجديد في بداية القائمة
        history.add(0, cleanedTerm)

        // التأكد من أن القائمة لا تتجاوز الحد الأقصى
        while (history.size > SearchHistoryManager.Companion.MAX_HISTORY_SIZE) {
            history.removeAt(history.size - 1)
        }

        saveSearchHistory(history)
    }

    // دالة لحذف مصطلح معين
    fun removeSearchTerm(term: String?) {
        val history = this.searchHistory
        history.remove(term)
        saveSearchHistory(history)
    }

    // دالة لمسح كل السجل
    fun clearHistory() {
        saveSearchHistory(ArrayList<String?>())
    }

    // دالة لحفظ القائمة في SharedPreferences
    private fun saveSearchHistory(history: MutableList<String?>?) {
        val json = gson.toJson(history)
        sharedPreferences.edit().putString(SearchHistoryManager.Companion.KEY_SEARCH_HISTORY, json)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "search_history_prefs"
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val MAX_HISTORY_SIZE = 15 // حد أقصى لعدد عمليات البحث المحفوظة
    }
}