package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.pages.auth

import android.os.Bundle
import android.util.Patterns
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
import com.google.firebase.firestore.FirebaseFirestore
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.databinding.FragmentSignUpBinding
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.extensions.passwordVisiblityToggle
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.config.FirebaseConfig
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.db.models.User

class SignUpFragment : Fragment() {
    private var _binding: FragmentSignUpBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth

        //        password visibility toggle
        binding.passwordCreateInput.passwordVisiblityToggle(requireContext())
        binding.confirmPassCreateInput.passwordVisiblityToggle(requireContext())

        binding.signUpBtn.setOnClickListener {
            val username = binding.usernameInput.text.toString()
            val email = binding.editTextTextEmailAddress.text.toString()
            val password = binding.passwordCreateInput.text.toString()
            val confirmPassword = binding.confirmPassCreateInput.text.toString()



            binding.usernameInput.error = null
            binding.editTextTextEmailAddress.error = null
            binding.passwordCreateInput.error = null
            binding.confirmPassCreateInput.error = null

            var isValid = true

            //            username validation
            if (username.isEmpty()) {
                binding.usernameInput.error = getString(R.string.username_empty)
                isValid = false
            }

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
                binding.passwordCreateInput.error = getString(R.string.password_empty)
                isValid = false
            } else if (confirmPassword.isEmpty()) {
                binding.confirmPassCreateInput.error = getString(R.string.password_empty)
                isValid = false
            } else if (password != confirmPassword) {
                binding.passwordCreateInput.error = getString(R.string.password_not_match)
                isValid = false
            } else if (!isPasswordStrong(password)) {
                binding.passwordCreateInput.error = getString(R.string.password_error)
                isValid = false
            }

            //            create account
            if (isValid) {
                createAccount(username, email, password)
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
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    //    check password strength
    private fun isPasswordStrong(password: String): Boolean {
        val passwordRegex =
            "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{6,}$".toRegex()
        return passwordRegex.matches(password)
    }

    //    create account
    private fun createAccount(username: String, email: String, password: String) {
        db = FirebaseConfig.getFirestore()
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(
            requireActivity()) { task: Task<AuthResult> ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    user?.let {
                        // Create a new user object
                        val newUser = User(it.uid, username, email)

                        // Store in Firestore
                        db.collection("users")
                            .document(it.uid) // Use user's UID as the document ID
                            .set(newUser)
                            .addOnSuccessListener {
                                findNavController()
                                    .navigate(R.id.action_signUpFragment_to_mainFragment)
                            }
                            .addOnFailureListener { e ->
                                // You can also show a Toast message to the user here if desired
                            }
                    }
                } else {
                    // Handle account creation failure
                    // You can also show a Toast message to the user here if desired
                }
            }
    }
}
