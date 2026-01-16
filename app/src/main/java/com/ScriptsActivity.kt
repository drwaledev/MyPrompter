package com.wtcb.myprompter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ScriptsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scripts)

        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        setupToolbar()
        setupViewPager()

        // Check if we should open a specific tab
        val tabToOpen = intent.getStringExtra("TAB")
        if (tabToOpen == "RECORDINGS") {
            viewPager.currentItem = 1
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViewPager() {
        adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter

        // Connect TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Scripts"
                1 -> "Recordings"
                else -> "Tab $position"
            }
        }.attach()
    }
}