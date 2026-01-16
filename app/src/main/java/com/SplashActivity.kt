package com.wtcb.myprompter

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_DELAY = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val prefs = getSharedPreferences("MyPrompterPrefs", MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("isFirstLaunch", true)
        val skippedSignIn = prefs.getBoolean("skippedSignIn", false)

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = when {
                isFirstLaunch -> Intent(this, WelcomeActivity::class.java)
                !skippedSignIn && !FirebaseAuthManager(this).isSignedIn() -> Intent(this, LoginActivity::class.java)
                else -> Intent(this, MainActivity::class.java)
            }
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, SPLASH_DELAY)
    }
}