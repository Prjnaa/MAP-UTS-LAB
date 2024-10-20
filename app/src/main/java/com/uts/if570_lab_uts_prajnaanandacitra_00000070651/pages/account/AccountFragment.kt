package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.pages.account

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.databinding.FragmentAccountBinding
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.extensions.getCurrentUserFromDB
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.config.FirebaseConfig
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.utils.sessionCheck

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

        sessionCheck()

        var username = ""
        var email = ""
        var nim = ""
        var imageUrl = ""

        auth = FirebaseAuth.getInstance()
        db = FirebaseConfig.getFirestore()
        storage = FirebaseConfig.getStorage()

        //        get user info from database
        auth.getCurrentUserFromDB(db) { user ->
            user?.let {

                username = it.username ?: "Not Set."
                nim = it.nim ?: "Not Set."
                email = it.email ?: "Not Set."
                imageUrl = it.imageUrl ?: ""

                //        show user info
                binding.usernameShow.text = username
                binding.editUsername.setText(username)
                binding.editNimText.setText(nim)
                binding.editEmail.setText(email)

                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .into(binding.userImage)
            }
        }

        // image upload icon
        binding.editImageIcon.setOnClickListener { galleryLauncher.launch("image/*") }

        //        save button
        binding.saveUserInfoBtn.setOnClickListener {
            val newUsername = binding.editUsername.text.toString()
            val newNim = binding.editNimText.text.toString()
            val newEmail = binding.editEmail.text.toString()

            if (newUsername.isNotEmpty() && newEmail.isNotEmpty() && newNim.isNotEmpty()) {
                saveUserUpdate(newUsername, newNim, newEmail)
            } else {
                //        show error message
            }
        }

        //        sign out button
        binding.signOutBtn.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.action_accountFragment_to_signInFragment)
        }

        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_accountFragment_to_mainFragment)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
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

    private fun saveUserUpdate(newUsername: String, newNim: String, newEmail: String) {
        auth.currentUser?.let {
            val newInfo =
                mapOf("username" to newUsername,
                    "nim" to newNim,
                    "email" to newEmail)

            db.collection("users")
                .document(it.uid)
                .update(newInfo)
                .addOnSuccessListener {
                    binding.usernameShow.text = newUsername
                    binding.editUsername.setText(newUsername)
                    binding.editNimText.setText(newNim)
                    binding.editEmail.setText(newEmail)
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
        val userId = auth.currentUser!!.uid
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
        auth.currentUser?.let {
            val userRef = db.collection("users").document(it.uid)

            userRef
                .update("imageUrl", imageUrl)
                .addOnSuccessListener {
                    Toast.makeText(
                            requireContext(), "Image uploaded successfully", Toast.LENGTH_SHORT)
                        .show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                            requireContext(),
                            "Image upload failed: ${e.message}",
                            Toast.LENGTH_SHORT)
                        .show()
                }
        }
    }
}
