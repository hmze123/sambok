package com.spidroid.starry;

import android.app.Application;

import com.bumptech.glide.Glide;
import com.bumptech.glide.MemoryCategory;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class StarryApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();

    Glide.get(this).setMemoryCategory(MemoryCategory.HIGH);
    FirebaseApp.initializeApp(this);

    FirebaseFirestore firestore = FirebaseFirestore.getInstance();
    FirebaseFirestoreSettings settings =
            new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true) // تم تمكين الثبات في وضع عدم الاتصال لتحسين تجربة المستخدم
                    .build();
    firestore.setFirestoreSettings(settings);
  }
}