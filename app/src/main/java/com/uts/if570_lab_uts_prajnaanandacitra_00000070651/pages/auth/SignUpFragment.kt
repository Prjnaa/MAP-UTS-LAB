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
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.databinding.FragmentSignUpBinding

class SignUpFragment : Fragment() {
    private var _binding: FragmentSignUpBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        binding.signUpBtn.setOnClickListener {
            val email = binding.editTextTextEmailAddress.text.toString()
            val password = binding.editTextTextPassword.text.toString()
            val confirmPassword = binding.editTextTextPasswordConfirm.text.toString()

            binding.editTextTextEmailAddress.error = null
            binding.editTextTextPassword.error = null
            binding.editTextTextPasswordConfirm.error = null

            var isValid = true

            //            email validation
            if (email.isEmpty()) {
                binding.editTextTextEmailAddress.error = getString(R.string.email_empty)
                isValid = false
            } else if (!isEmailValid(email)) {
                binding.editTextTextEmailAddress.error = getString(R.string.email_invalid)
                isValid = false
            }

            //            password validation
            if (password.isEmpty()) {
                binding.editTextTextPassword.error = getString(R.string.password_empty)
                isValid = false
            } else if (confirmPassword.isEmpty()) {
                binding.editTextTextPasswordConfirm.error = getString(R.string.password_empty)
                isValid = false
            } else if (password != confirmPassword) {
                binding.editTextTextPassword.error = getString(R.string.password_not_match)
                isValid = false
            } else if (!isPasswordStrong(password)) {
                binding.editTextTextPassword.error = getString(R.string.password_error)
                isValid = false
            }

            //            create account
            if (isValid) {
                createAccount(email, password)
                findNavController().navigate(R.id.action_signUpFragment_to_mainFragment)
            }
        }

        binding.signInRedirect.setOnClickListener {
            findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

//    check email validity
    private fun isEmailValid(email: String): Boolean {
        val emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }

//    check password strength
    private fun isPasswordStrong(password: String): Boolean {
        val passwordRegex =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,}$".toRegex()
        return passwordRegex.matches(password)
    }

//    create account
    private fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task: Task<AuthResult> ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                } else {

                }
            }
    }
}