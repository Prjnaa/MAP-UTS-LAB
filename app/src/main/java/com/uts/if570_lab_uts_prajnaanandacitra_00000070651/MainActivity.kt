package com.uts.if570_lab_uts_prajnaanandacitra_00000070651

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.databinding.ActivityMainBinding
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.config.FirebaseConfig

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseConfig.initializeApp(this)

        auth = Firebase.auth

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.signInFragment, R.id.signUpFragment -> {
                    // Hide bottom navigation when on Sign In or Sign Up
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    // Show bottom navigation for other fragments
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
            }
        }

        if (auth.currentUser != null) {
            navController.navigate(R.id.mainFragment)
        } else {
            navController.navigate(R.id.signInFragment)
        }
    }
}
