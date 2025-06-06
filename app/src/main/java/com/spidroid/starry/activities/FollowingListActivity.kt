package com.spidroid.starry.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class FollowingListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // إعادة استخدام نفس المنطق والـ layout الخاص بـ FollowersListActivity
        val intent = Intent(this, FollowersListActivity::class.java)
        intent.putExtra(
            FollowersListActivity.Companion.EXTRA_USER_ID,
            getIntent().getStringExtra(FollowersListActivity.Companion.EXTRA_USER_ID)
        )
        intent.putExtra(FollowersListActivity.Companion.EXTRA_LIST_TYPE, "following")
        startActivity(intent)
        finish()
    }
}