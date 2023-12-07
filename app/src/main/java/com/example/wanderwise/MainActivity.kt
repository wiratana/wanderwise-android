package com.example.wanderwise

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.wanderwise.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment, R.id.postFragment, R.id.rankFragment, R.id.profileFragment
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        navView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.homeFragment -> {
                    findNavController(R.id.nav_host_fragment_activity_main).navigate(R.id.homeFragment)
                    true
                }
                R.id.rankFragment -> {
                    findNavController(R.id.nav_host_fragment_activity_main).navigate(R.id.rankFragment)
                    true
                }
                R.id.postFragment -> {
                    findNavController(R.id.nav_host_fragment_activity_main).navigate(R.id.postFragment)
                    true
                }
                R.id.profileFragment -> {
                    findNavController(R.id.nav_host_fragment_activity_main).navigate(R.id.profileFragment)
                    true
                }
                else -> false
            }
        }

        supportActionBar?.hide()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)

        if (currentFragment is HomeFragment) {
            finishAffinity()
        }
    }
}