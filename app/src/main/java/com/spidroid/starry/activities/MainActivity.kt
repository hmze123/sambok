package com.spidroid.starry.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ActivityMainBinding

// << تأكد من هذا الاستيراد
class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(getLayoutInflater())
        setContentView(binding!!.getRoot())

        // --- بداية التعديل ---
        // 1. احصل على NavHostFragment من خلال ID الخاص به في ملف التخطيط.
        val navHostFragment = getSupportFragmentManager()
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment?

        // 2. تحقق من أن navHostFragment ليس فارغًا (null) قبل المتابعة.
        if (navHostFragment != null) {
            // 3. احصل على NavController من NavHostFragment.
            val navController = navHostFragment.navController

            // إعداد AppBarConfiguration (إذا كنت تستخدم ActionBar مع NavController)
            val appBarConfiguration =
                AppBarConfiguration.Builder(
                    R.id.navigation_home,
                    R.id.navigation_search,
                    R.id.navigation_notifications,
                    R.id.navigation_messages
                )
                    .build()

            // 4. قم بإعداد BottomNavigationView مع NavController.
            setupWithNavController(binding!!.navView, navController)

            // إذا كنت تستخدم ActionBar يتم التحكم به بواسطة NavController، يمكنك إضافة:
            // NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            // وإلا، هذا السطر ليس ضروريًا إذا كان شريط التنقل السفلي هو الأساس.
        } else {
            // في حالة عدم العثور على NavHostFragment، يمكنك إضافة تسجيل خطأ هنا أو رسالة.
            // مثال:
            // android.util.Log.e("MainActivity", "Critical Error: NavHostFragment not found.");
            // android.widget.Toast.makeText(this, "Error initializing navigation components.", android.widget.Toast.LENGTH_LONG).show();
            // finish(); // قد يكون من المناسب إغلاق التطبيق إذا كان التنقل ضروريًا.
        }
        // --- نهاية التعديل ---
    }
}