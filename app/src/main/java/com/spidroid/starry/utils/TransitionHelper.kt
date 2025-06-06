package com.spidroid.starry.utils

import android.app.Activity
import android.transition.TransitionInflater
import com.spidroid.starry.R

object TransitionHelper {
    fun setupSharedElementTransition(activity: Activity) {
        val transition = TransitionInflater.from(activity)
            .inflateTransition(R.transition.image_transition)
        activity.getWindow().setSharedElementEnterTransition(transition)
        activity.getWindow().setSharedElementReturnTransition(transition)
    }
}