package com.example.vidhyavahini.ui.splash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.vidhyavahini.R
import com.example.vidhyavahini.databinding.ActivitySplashBinding
import com.example.vidhyavahini.ui.home.HomeActivity
import com.example.vidhyavahini.ui.onboarding.OnboardingActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure the window to fit system windows
        WindowCompat.setDecorFitsSystemWindows(window, true)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary)


        // Apply padding to the root view to prevent content from being drawn under the status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, insets.top, insets.right, insets.bottom)
            windowInsets
        }

        Handler(Looper.getMainLooper()).postDelayed({
            checkOnboardingState()
        }, 2000)
    }

    private fun checkOnboardingState() {
        val sharedPref = getSharedPreferences("vidya_vahini_prefs", Context.MODE_PRIVATE)
        val isSetupDone = sharedPref.getBoolean("isSetupDone", false)

        if (isSetupDone) {
            startActivity(Intent(this, HomeActivity::class.java))
        } else {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
        finish()
    }
}
