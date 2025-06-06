// hmze123/sambok/sambok-main/app/src/main/java/com/spidroid/starry/activities/MainActivity.kt
package com.spidroid.starry.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private companion object {
        private const val TAG = "MainActivity" // ✨ تم تعريف TAG
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment

        val navController: NavController = navHostFragment.navController

        // Setup the bottom navigation view with navController
        binding.navView.setupWithNavController(navController)
    }
}