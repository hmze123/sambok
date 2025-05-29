package com.spidroid.starry.utils;

import android.app.Activity;
import android.transition.Transition;
import android.transition.TransitionInflater;
import com.spidroid.starry.R;

public class TransitionHelper {
    
    public static void setupSharedElementTransition(Activity activity) {
        Transition transition = TransitionInflater.from(activity)
                .inflateTransition(R.transition.image_transition);
        activity.getWindow().setSharedElementEnterTransition(transition);
        activity.getWindow().setSharedElementReturnTransition(transition);
    }
}