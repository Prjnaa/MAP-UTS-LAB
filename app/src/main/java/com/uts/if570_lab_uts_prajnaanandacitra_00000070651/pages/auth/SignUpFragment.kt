package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.pages.auth

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.databinding.FragmentSignUpBinding
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.extensions.passwordVisibilityToggle
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.db.models.Attendance
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
        _binding = FragmentSignUpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        with(binding) {
            passwordCreateInput.passwordVisibilityToggle(requireContext())

            confirmPassCreateInput.passwordVisibilityToggle(requireContext())

            signUpBtn.setOnClickListener { validateAndSignUp() }

            signInRedirect.setOnClickListener {
                findNavController().navigate(R.id.action_signUpFragment_to_signInFragment)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    //    validate and sign up
    private fun validateAndSignUp() {
        val username = binding.usernameInput.text.toString()
        val email = binding.editTextTextEmailAddress.text.toString()
        val nim = binding.nimInput.text.toString()
        val password = binding.passwordCreateInput.text.toString()
        val confirmPassword = binding.confirmPassCreateInput.text.toString()

        when {
            username.isEmpty() -> setError(binding.usernameInput, R.string.username_empty)
            nim.isEmpty() -> setError(binding.nimInput, R.string.nim_empty)
            email.isEmpty() -> setError(binding.editTextTextEmailAddress, R.string.email_empty)
            !isEmailValid(email) ->
                setError(binding.editTextTextEmailAddress, R.string.email_invalid)
            password.isEmpty() -> setError(binding.passwordCreateInput, R.string.password_empty)
            confirmPassword.isEmpty() ->
                setError(binding.confirmPassCreateInput, R.string.password_empty)
            password != confirmPassword ->
                setError(binding.passwordCreateInput, R.string.password_not_match)
            !isPasswordStrong(password) ->
                setError(binding.passwordCreateInput, R.string.password_error)
            else -> createAccount(username, nim, email, password)
        }
    }

    private fun setError(view: View, messageResId: Int) {
        (view as? EditText)?.error = getString(messageResId)
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
    private fun createAccount(
        username: String,
        studentId: String,
        email: String,
        password: String
    ) {
        binding.signUpBtn.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(
            requireActivity()) { task: Task<AuthResult> ->
                if (task.isSuccessful) {
                    val user = auth.currentUser

                    user?.let {
                        it.email?.let { email -> sendVerificationEmail(email) }

                        // Create a new user object
                        val newUser = User(username, studentId, "")

                        // Store in Firestore
                        db.collection("users").document(it.uid).set(newUser).addOnSuccessListener {
                            initializeUserAttendance(user.uid)

                            auth.signOut()

                            Toast.makeText(
                                    requireContext(),
                                    "Verification email sent to ${email}. Please verify your email before logging in.",
                                    Toast.LENGTH_LONG)
                                .show()

                            findNavController()
                                .navigate(R.id.action_signUpFragment_to_signInFragment)
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Sign Up Failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun sendVerificationEmail(email: String) {
        auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Toast.makeText(
                        requireContext(), "Failed to send verification email.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun initializeUserAttendance(userId: String) {
        val attendance = Attendance(userId, emptyList())

        db.collection("attendance").document(userId).set(attendance).addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Attendance State Creation Failed", Toast.LENGTH_SHORT)
                .show()
        }
    }
}
