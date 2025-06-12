package com.spidroid.starry.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController

        binding.navView.setupWithNavController(navController)

        // ١. استدعاء الدالة الجديدة لإعداد أزرار شريط الأدوات
        setupToolbarListeners()
    }

    /**
     * دالة جديدة لإعداد أوامر الضغط على عناصر شريط الأدوات
     */
    private fun setupToolbarListeners() {
        // الوصول إلى عناصر شريط الأدوات من خلال binding.mainToolbar
        // main_toolbar هو الـ id الخاص بالـ <include> في activity_main.xml

        // عند الضغط على أيقونة الإشعارات
        binding.mainToolbar.toolbarNotificationsIcon.setOnClickListener {
            // انتقل إلى واجهة الإشعارات
            binding.navView.selectedItemId = R.id.navigation_notifications
        }

        // عند الضغط على شريط البحث
        binding.mainToolbar.toolbarSearch.setOnClickListener {
            // انتقل إلى واجهة البحث
            binding.navView.selectedItemId = R.id.navigation_search
        }
    }
}