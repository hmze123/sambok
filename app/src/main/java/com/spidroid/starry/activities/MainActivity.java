package com.spidroid.starry.activities;

import android.os.Bundle;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment; // << تأكد من هذا الاستيراد
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.spidroid.starry.R;
import com.spidroid.starry.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- بداية التعديل ---
        // 1. احصل على NavHostFragment من خلال ID الخاص به في ملف التخطيط.
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_activity_main);

        // 2. تحقق من أن navHostFragment ليس فارغًا (null) قبل المتابعة.
        if (navHostFragment != null) {
            // 3. احصل على NavController من NavHostFragment.
            NavController navController = navHostFragment.getNavController();

            // إعداد AppBarConfiguration (إذا كنت تستخدم ActionBar مع NavController)
            AppBarConfiguration appBarConfiguration =
                    new AppBarConfiguration.Builder(
                            R.id.navigation_home,
                            R.id.navigation_search,
                            R.id.navigation_notifications,
                            R.id.navigation_messages)
                            .build();

            // 4. قم بإعداد BottomNavigationView مع NavController.
            NavigationUI.setupWithNavController(binding.navView, navController);

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