package com.spidroid.starry;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.StorageReference;

import java.io.InputStream;

@GlideModule
public class MyAppGlideModule extends AppGlideModule {
  @Override
  public void applyOptions(Context context, GlideBuilder builder) {
    builder.setDefaultRequestOptions(
        new RequestOptions()
            .format(DecodeFormat.PREFER_RGB_565) // Reduce memory usage
            .diskCacheStrategy(DiskCacheStrategy.ALL));
  }

  @Override
  public void registerComponents(Context context, Glide glide, Registry registry) {
    registry.append(StorageReference.class, InputStream.class, new FirebaseImageLoader.Factory());
  }

  @Override
  public boolean isManifestParsingEnabled() {
    return false;
  }
}
