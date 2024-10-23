package com.uts.if570_lab_uts_prajnaanandacitra_00000070651

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.databinding.ActivityLandingBinding

class LandingActivity : AppCompatActivity() {
    private var _binding: ActivityLandingBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        _binding = ActivityLandingBinding.inflate(layoutInflater, null, false)
        setContentView(binding.root)

        binding.getStartedButton.setOnClickListener {
            val mainIntent = Intent(this, MainActivity::class.java)
            mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(mainIntent)
        }

        auth = Firebase.auth
        if (auth.currentUser != null) {
            val mainIntent = Intent(this, MainActivity::class.java)
            mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(mainIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
