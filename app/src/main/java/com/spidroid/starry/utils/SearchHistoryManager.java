package com.spidroid.starry.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SearchHistoryManager {

    private static final String PREFS_NAME = "search_history_prefs";
    private static final String KEY_SEARCH_HISTORY = "search_history";
    private static final int MAX_HISTORY_SIZE = 15; // حد أقصى لعدد عمليات البحث المحفوظة

    private final SharedPreferences sharedPreferences;
    private final Gson gson = new Gson();

    public SearchHistoryManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // دالة لجلب قائمة البحث
    public List<String> getSearchHistory() {
        String json = sharedPreferences.getString(KEY_SEARCH_HISTORY, null);
        if (json == null) {
            return new ArrayList<>();
        }
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    // دالة لإضافة مصطلح بحث جديد
    public void addSearchTerm(String term) {
        if (term == null || term.trim().isEmpty()) {
            return;
        }
        String cleanedTerm = term.trim();
        List<String> history = getSearchHistory();

        // إزالة المصطلح إذا كان موجوداً بالفعل لإضافته في الأعلى
        history.remove(cleanedTerm);

        // إضافة المصطلح الجديد في بداية القائمة
        history.add(0, cleanedTerm);

        // التأكد من أن القائمة لا تتجاوز الحد الأقصى
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(history.size() - 1);
        }

        saveSearchHistory(history);
    }

    // دالة لحذف مصطلح معين
    public void removeSearchTerm(String term) {
        List<String> history = getSearchHistory();
        history.remove(term);
        saveSearchHistory(history);
    }

    // دالة لمسح كل السجل
    public void clearHistory() {
        saveSearchHistory(new ArrayList<>());
    }

    // دالة لحفظ القائمة في SharedPreferences
    private void saveSearchHistory(List<String> history) {
        String json = gson.toJson(history);
        sharedPreferences.edit().putString(KEY_SEARCH_HISTORY, json).apply();
    }
}