package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.pages.main

import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.api.ImageResponse
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.api.RetrofitClient
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.databinding.FragmentMainBinding
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.config.FirebaseConfig
import java.time.LocalTime
import retrofit2.Call

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val apiKey = "m2DdgPvD0LGpOO2vRFYMjBWxuzr-NILnAMTWKthilrg"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        val currentUser = auth.currentUser

        //        session check
        if (currentUser == null) {
            findNavController().navigate(R.id.action_mainFragment_to_signInFragment)
            return
        }

        //        show greetings based on time
        binding.greetings.text = getGreetings()

        //        show current user username
        getCurrentUserFromDB(currentUser.uid) { username ->
            if (username != null) {
                binding.userNameView.text = username
            } else {
                binding.userNameView.text = "Username Not Set."
            }
        }

        //        fetching image from api
        fetchRandomImage()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun getCurrentUserFromDB(uid: String, onResult: (String?) -> Unit) {
        db = FirebaseConfig.getFirestore()

        val user = db.collection("users").document(uid)
        user
            .get()
            .addOnSuccessListener { document ->
                val username = document.getString("username")
                onResult(username)
            }
            .addOnFailureListener { onResult(null) }
    }

    private fun getGreetings(): String {
        return when (LocalTime.now().hour) {
            in 3..11 -> "Good Morning"
            in 12..15 -> "Good Afternoon"
            else -> "Good Evening"
        }
    }

    private fun iconState() {
        when (LocalTime.now().hour) {
            in 6..17 -> {
                binding.timeIcon.setImageResource(R.drawable.baseline_sunny_40)
                binding.timeIcon.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.sun), PorterDuff.Mode.SRC_IN)
            }
            else -> {
                binding.timeIcon.setImageResource(R.drawable.baseline_bedtime_40)
                binding.timeIcon.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.moon), PorterDuff.Mode.SRC_IN)
            }
        }
    }

    private fun fetchRandomImage() {
        val apiService = RetrofitClient.instance
        val call = apiService.getRandomImage(apiKey)

        call.enqueue(
            object : retrofit2.Callback<ImageResponse> {
                override fun onResponse(
                    call: Call<ImageResponse>,
                    response: retrofit2.Response<ImageResponse>
                ) {
                    if (response.isSuccessful) {
                        response.body()?.let { imageResponse ->
                            Glide.with(this@MainFragment)
                                .load(imageResponse.urls.regular)
                                .into(binding.imageView)
                        }
                    }
                }

                override fun onFailure(call: Call<ImageResponse>, t: Throwable) {
                    // Handle failure
                    t.printStackTrace()
                }
            })
    }
}
