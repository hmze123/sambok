package com.spidroid.starry.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class FollowingListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // إعادة استخدام نفس المنطق والـ layout الخاص بـ FollowersListActivity
        Intent intent = new Intent(this, FollowersListActivity.class);
        intent.putExtra(FollowersListActivity.EXTRA_USER_ID, getIntent().getStringExtra(FollowersListActivity.EXTRA_USER_ID));
        intent.putExtra(FollowersListActivity.EXTRA_LIST_TYPE, "following");
        startActivity(intent);
        finish();
    }
}