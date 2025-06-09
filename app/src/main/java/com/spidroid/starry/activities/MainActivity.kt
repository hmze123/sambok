package com.spidroid.starry.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.spidroid.starry.R
import com.spidroid.starry.databinding.ActivityMainBinding
import com.spidroid.starry.repositories.UserRepository
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val userRepository = UserRepository()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // No action needed here for now
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController: NavController = navHostFragment.navController
        val navView: BottomNavigationView = binding.navView

        navView.setupWithNavController(navController)
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    intent.putExtra("userId", FirebaseAuth.getInstance().currentUser?.uid)
                    startActivity(intent)
                    true
                }
                R.id.navigation_home -> {
                    navController.navigate(R.id.navigation_home)
                    true
                }
                R.id.navigation_search -> {
                    navController.navigate(R.id.navigation_search)
                    true
                }
                R.id.navigation_messages -> {
                    navController.navigate(R.id.navigation_messages)
                    true
                }
                R.id.navigation_notifications -> {
                    navController.navigate(R.id.navigation_notifications)
                    true
                }
                else -> false
            }
        }

        askNotificationPermission()
        // استدعاء الدالة الجديدة لتحميل ومراقبة الصورة
        loadAndObserveProfileImage()
    }
    private fun loadAndObserveProfileImage() {
        Log.d("DirectDebug", "بدء دالة تحميل الصورة...")

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId != null) {
            Log.d("DirectDebug", "تم العثور على ID المستخدم: $currentUserId")
            Log.d("DirectDebug", "جاري استدعاء getUserById من الـ Repository...")

            // استدعاء الدالة الصحيحة ومراقبة النتيجة
            userRepository.getUserById(currentUserId).observe(this) { userModel ->
                Log.d("DirectDebug", "تم استلام بيانات من LiveData.")

                if (userModel != null) {
                    Log.d("DirectDebug", "بيانات المستخدم ليست فارغة. رابط الصورة: ${userModel.profileImageUrl}")

                    val menuItem = binding.navView.menu.findItem(R.id.navigation_profile)
                    val actionView = menuItem.actionView
                    val profileImageView = actionView?.findViewById<CircleImageView>(R.id.nav_profile_image)

                    if (profileImageView == null) {
                        Log.e("DirectDebug", "خطأ: لم يتم العثور على profileImageView!")
                        return@observe
                    }

                    Log.d("DirectDebug", "تم العثور على ImageView. جاري استدعاء Glide...")
                    profileImageView.let { imageView ->
                        // نتحقق إذا كان الرابط فارغًا (null) أو خاليًا
                        if (userModel.profileImageUrl.isNullOrEmpty()) {
                            // إذا كان فارغًا، نعرض الصورة الافتراضية مباشرة
                            imageView.setImageResource(R.drawable.ic_home_selector)
                          //  imageView.setImageResource(R.drawable.ic_profile_selector)
                            Log.d("DirectDebug", "الرابط فارغ، تم تعيين الصورة الافتراضية.")
                        } else {
                            // إذا لم يكن فارغًا، نستخدم Glide لتحميله
                            Glide.with(this@MainActivity)
                                .load(userModel.profileImageUrl)
                                .placeholder(R.drawable.ic_profile_selector)
                                .error(R.drawable.ic_profile_selector)
                                .into(imageView)
                            Log.d("DirectDebug", "الرابط موجود، جاري تحميل الصورة باستخدام Glide.")
                        }
                    }
                } else {
                    Log.w("DirectDebug", "تحذير: البيانات التي وصلت من LiveData فارغة (null).")
                }
            }
        } else {
            Log.e("DirectDebug", "خطأ: لم يتم العثور على ID المستخدم الحالي. المستخدم لم يسجل دخوله؟")
        }
    }
   /* private fun loadAndObserveProfileImage() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        if (currentUserId != null) {
            // استدعاء الدالة الصحيحة getUserById ومراقبة النتيجة
            userRepository.getUserById(currentUserId).observe(this) { userModel ->
                if (userModel != null) {
                    val menuItem = binding.navView.menu.findItem(R.id.navigation_profile)
                    val actionView = menuItem.actionView
                    val profileImageView = actionView?.findViewById<CircleImageView>(R.id.nav_profile_image)

                    profileImageView?.let { imageView ->
                        Glide.with(this@MainActivity)
                            .load(userModel.profileImageUrl) // استخدام الاسم الصحيح
                            .placeholder(R.drawable.ic_profile_selector)
                            .error(R.drawable.ic_profile_selector)
                            .fallback(R.drawable.ic_profile_selector)
                            .into(imageView)
                    }
                }
            }
        }
    }*/
}