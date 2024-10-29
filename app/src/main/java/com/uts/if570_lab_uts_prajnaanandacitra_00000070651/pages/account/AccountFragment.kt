package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.pages.account

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.actionCodeSettings
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.databinding.FragmentAccountBinding
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.extensions.getCurrentUserFromDB
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.config.FirebaseConfig
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.utils.deleteAccount
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.utils.showChangePassDialog
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.utils.showPassDialog

class AccountFragment : Fragment() {
    private var _binding: FragmentAccountBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var userImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        db = FirebaseConfig.getFirestore()
        storage = FirebaseConfig.getStorage()

        loadUserData()
        setupClickListeners()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun loadUserData() {
        auth.getCurrentUserFromDB(db) { user ->
            user?.let {
                binding.usernameShow.text = it.username
                binding.editUsername.setText(it.username)
                binding.editNimText.setText(it.studentId)

                Glide.with(requireContext())
                    .load(it.imageUrl)
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .into(binding.userImage)
            }
        }

        auth.currentUser?.let { user -> binding.editEmail.setText(user.email) }
    }

    private fun setupClickListeners() {
        with(binding) {
            editImageIcon.setOnClickListener { galleryLauncher.launch("image/*") }

            deleteAccBtn.setOnClickListener { onDeleteAccount() }

            saveUserInfoBtn.setOnClickListener {
                val newUsername = editUsername.text.toString()
                val newNim = editNimText.text.toString()
                val newEmail = editEmail.text.toString()

                if (newUsername.isNotEmpty() && newEmail.isNotEmpty() && newNim.isNotEmpty()) {
                    reAuthUser(newUsername, newNim, newEmail)
                } else {
                    Toast.makeText(
                            requireContext(),
                            "Make sure all fields are filled.",
                            Toast.LENGTH_SHORT)
                        .show()
                }
            }

            changePassBtn.setOnClickListener { showChangePassDialog(requireContext()) }

            signOutBtn.setOnClickListener {
                auth.signOut()
                findNavController().navigate(R.id.action_accountFragment_to_signInFragment)
            }

            backButton.setOnClickListener {
                findNavController().navigate(R.id.action_accountFragment_to_mainFragment)
            }
        }
    }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                userImageUri = it
                binding.userImage.setImageURI(it)
                uploadImageToStorage(it)
            }
                ?: run {
                    Toast.makeText(requireContext(), "Image selection canceled", Toast.LENGTH_SHORT)
                        .show()
                }
        }

    private fun onDeleteAccount() {
        deleteAccount(auth, db, storage) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT)
                    .show()
                findNavController().navigate(R.id.action_global_signInFragment)
            } else {
                Toast.makeText(requireContext(), "Failed to delete account", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun reAuthUser(newUsername: String, newNim: String, newEmail: String) {
        showPassDialog(requireContext()) { password ->
            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(
                        requireContext(), "No user is currently signed in", Toast.LENGTH_SHORT)
                    .show()
                return@showPassDialog
            }

            val userEmail = user.email
            if (userEmail == null) {
                Toast.makeText(requireContext(), "User email not found", Toast.LENGTH_SHORT).show()
                return@showPassDialog
            }

            val credentials = EmailAuthProvider.getCredential(userEmail, password)

            user
                .reauthenticate(credentials)
                .addOnSuccessListener {
                    if (userEmail != newEmail) {
                        // Only update email if it's different from current email
                        sendSignInLink(newEmail) { success ->
                            if (success) {
                                Toast.makeText(
                                        requireContext(),
                                        "Confirmation link sent. Please check your email.",
                                        Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    } else {
                        saveUserUpdate(newUsername, newNim)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                            requireContext(),
                            "Authentication failed: ${e.message}",
                            Toast.LENGTH_LONG)
                        .show()
                }
        }
    }

    private fun sendSignInLink(email: String, onComplete: (Boolean) -> Unit) {
        val actionCodeSettings = actionCodeSettings {
            url = "https://unionappmap.page.link/updateEmail"
            handleCodeInApp = true
            setIOSBundleId("com.uts.if570_lab_uts_prajnaanandacitra_00000070651")
            setAndroidPackageName(
                "com.uts.if570_lab_uts_prajnaanandacitra_00000070651.pages.account", true, "12")
        }

        auth.sendSignInLinkToEmail(email, actionCodeSettings).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onComplete(true)
            } else {
                Toast.makeText(
                        requireContext(),
                        "Failed to send link: ${task.exception?.message}",
                        Toast.LENGTH_LONG)
                    .show()
                onComplete(false)
            }
        }
    }

    private fun updateEmail(user: FirebaseUser, newEmail: String, onComplete: (Boolean) -> Unit) {
        if (!user.isEmailVerified) {
            Toast.makeText(
                    requireContext(),
                    "Please verify your current email before changing to a new one",
                    Toast.LENGTH_LONG)
                .show()
            onComplete(false)
            return
        }

        user
            .updateEmail(newEmail)
            .addOnSuccessListener {
                // Send verification email to new address
                user.sendEmailVerification().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        binding.editEmail.setText(newEmail)
                        Toast.makeText(
                                requireContext(),
                                "Email updated. Please verify your new email address.",
                                Toast.LENGTH_LONG)
                            .show()
                        onComplete(true)
                    } else {
                        Toast.makeText(
                                requireContext(),
                                "Failed to send verification email: ${task.exception?.message}",
                                Toast.LENGTH_LONG)
                            .show()
                        onComplete(false)
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                        requireContext(), "Failed to update email: ${e.message}", Toast.LENGTH_LONG)
                    .show()
                onComplete(false)
            }
    }

    private fun saveUserUpdate(newUsername: String, newNim: String) {
        auth.currentUser?.let { user ->
            val newInfo = mapOf("username" to newUsername, "studentId" to newNim)

            db.collection("users")
                .document(user.uid)
                .update(newInfo)
                .addOnSuccessListener {
                    binding.usernameShow.text = newUsername
                    binding.editUsername.setText(newUsername)
                    binding.editNimText.setText(newNim)
                    Toast.makeText(
                            requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT)
                        .show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                            requireContext(), "Update failed: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }

    private fun uploadImageToStorage(imageUri: Uri) {
        val storageRef = storage.reference
        val userId = auth.currentUser?.uid ?: return
        val imageRef = storageRef.child("image/${userId}.jpg")

        imageRef
            .putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    saveImageUrlToFirestore(downloadUri.toString())
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                        requireContext(), "Image upload failed: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun saveImageUrlToFirestore(imageUrl: String) {
        auth.currentUser?.let { user ->
            db.collection("users")
                .document(user.uid)
                .update("imageUrl", imageUrl)
                .addOnSuccessListener {
                    Toast.makeText(
                            requireContext(), "Image uploaded successfully", Toast.LENGTH_SHORT)
                        .show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                            requireContext(),
                            "Failed to update profile image: ${e.message}",
                            Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }
}
