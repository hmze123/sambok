package com.spidroid.starry.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SearchViewModel : ViewModel() {
    private val mText: MutableLiveData<String?>

    init {
        mText = MutableLiveData<String?>()
        mText.setValue("This is search fragment")
    }

    val text: LiveData<String?>
        get() = mText
}