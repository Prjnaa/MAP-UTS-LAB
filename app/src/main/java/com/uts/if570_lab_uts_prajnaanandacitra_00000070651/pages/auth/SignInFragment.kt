package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.pages.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.databinding.FragmentSignInBinding

class SignInFragment : Fragment() {
    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        var currentUser = auth.currentUser

//        session check
        if(currentUser != null) {
            findNavController().navigate(R.id.action_signInFragment_to_mainFragment)
            return
        }

//        sign in button
        binding.signInButton.setOnClickListener {
            val email = binding.emailInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if(email.isNotEmpty()) {
                if(password.isNotEmpty()) {
                    signIn(email, password)
                } else {
//                    password empty
                }
            } else {
//                email empty
            }

            findNavController().navigate(R.id.action_signInFragment_to_mainFragment)
        }

        binding.signUpRedirect.setOnClickListener {
            findNavController().navigate(R.id.action_signInFragment_to_signUpFragment)
        }
    }

    private fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task: Task<AuthResult> ->
                if(task.isSuccessful) {
                    findNavController().navigate(R.id.action_signInFragment_to_mainFragment)
                } else {
//                        sign in error
                }
            }
    }
}
