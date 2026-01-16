package com.wtcb.myprompter

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class WelcomeActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: MaterialButton
    private lateinit var btnSkip: Button

    private val slides = listOf(
        WelcomeSlide(
            "📹",
            "Welcome to MyPrompter",
            "Your professional teleprompter app for seamless video recording with scrolling text"
        ),
        WelcomeSlide(
            "🎬",
            "Record Like a Pro",
            "Camera preview with auto-scrolling text. Adjust speed, font size, and colors to match your needs"
        ),
        WelcomeSlide(
            "⭐",
            "Earn Points, Extend Limits",
            "Watch rewarded videos to earn points. Use points to extend your word limit and create longer scripts"
        ),
        WelcomeSlide(
            "🚀",
            "Let's Get Started!",
            "Sign in with Google to sync your points across all devices, or skip to continue"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)

        val adapter = WelcomeSlideAdapter(slides)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        btnNext.setOnClickListener {
            if (viewPager.currentItem < slides.size - 1) {
                viewPager.currentItem += 1
            } else {
                finishWelcome()
            }
        }

        btnSkip.setOnClickListener {
            finishWelcome()
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == slides.size - 1) {
                    btnNext.text = "Continue"
                    btnSkip.visibility = View.GONE
                } else {
                    btnNext.text = "Next"
                    btnSkip.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun finishWelcome() {
        getSharedPreferences("MyPrompterPrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("isFirstLaunch", false)
            .apply()

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

data class WelcomeSlide(
    val emoji: String,
    val title: String,
    val description: String
)