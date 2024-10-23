package com.uts.if570_lab_uts_prajnaanandacitra_00000070651

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.databinding.ActivityMainBinding
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.config.FirebaseConfig

class MainActivity : AppCompatActivity() {
    private var _binding: ActivityMainBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var navController: NavController

    private val auth by lazy { Firebase.auth }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FirebaseConfig.initializeApp(this)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
    }

    override fun onResume() {
        super.onResume()

        if (auth.currentUser == null) {
            navController.navigate(R.id.action_global_signInFragment)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
