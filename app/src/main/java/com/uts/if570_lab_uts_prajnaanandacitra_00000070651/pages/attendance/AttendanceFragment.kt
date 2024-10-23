package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.pages.attendance

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.databinding.FragmentAttendanceBinding
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.config.FirebaseConfig
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.db.models.Attendance
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.db.models.AttendanceItem
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceFragment : Fragment() {
    private var _binding: FragmentAttendanceBinding? = null
    private val binding
        get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var imageCapture: ImageCapture? = null
    private var imageBitmap: Bitmap? = null
    private var imageUri: Uri? = null
    private var isCheckIn: Boolean? = false

    private val tag = "AttendanceFragment"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = Firebase.auth
        db = FirebaseConfig.getFirestore()
        storage = FirebaseConfig.getStorage()

        handleCameraPermission()
        updateUserCheckInState()

        binding.apply {
            btnCapture.setOnClickListener { takePhoto() }
            backButtonFromAttendance.setOnClickListener { navigateBack() }
            btnUpload.setOnClickListener { uploadPhotoFromGallery() }
            btnSubmit.setOnClickListener { submitAttendance() }
        }
    }

    private fun updateUserCheckInState() {
        getIsUserCheckedInFromDB { checkInState ->
            isCheckIn = checkInState
            binding.btnSubmit.text =
                getString(if (isCheckIn == true) R.string.submit_in else R.string.submit_out)
            binding.discardImgBtn.visibility = View.GONE
        }
    }

    private fun getIsUserCheckedInFromDB(callback: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = db.collection("attendance").document(uid)
        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())

        userRef
            .get()
            .addOnSuccessListener { document ->
                isCheckIn =
                    if (document.exists()) {
                        val attendance = document.toObject(Attendance::class.java)
                        attendance?.attendanceList?.any {
                            it.date == currentDate && it.checkInTime.isNotEmpty()
                        } ?: false
                    } else {
                        false
                    }
                callback(isCheckIn!!)
            }
            .addOnFailureListener { callback(false) }
    }

    private fun handleCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionRequestLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val cameraPermissionRequestLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startCamera() else showToast("Camera permission denied")
        }

    private fun startCamera() {
        ProcessCameraProvider.getInstance(requireActivity())
            .addListener(
                {
                    val cameraProvider = ProcessCameraProvider.getInstance(requireActivity()).get()
                    val preview =
                        Preview.Builder().build().also {
                            it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                        }
                    imageCapture = ImageCapture.Builder().build()

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                    } catch (ex: Exception) {
                        showToast(ex.message ?: "Error starting camera")
                    }
                },
                ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        imageCapture?.let { capture ->
            val uid = auth.currentUser?.uid ?: "unknown_user"
            val currentDateTime =
                SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault())
                    .format(System.currentTimeMillis())
            val attendanceType = if (isCheckIn == true) "check_in" else "check_out"
            val filename = "${uid}_${currentDateTime}_${attendanceType}.jpg"

            val outputOptions =
                ImageCapture.OutputFileOptions.Builder(createImageFile(filename)).build()
            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        outputFileResults.savedUri?.let { uri ->
                            imageBitmap = decodeBitmapFromUri(uri)
                            updateImagePreview(uri)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        showToast("Photo capture failed")
                    }
                })
        }
    }

    private fun decodeBitmapFromUri(uri: Uri): Bitmap? {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(requireContext().contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
        }
    }

    private fun updateImagePreview(uri: Uri) {
        binding.imgPreview.setImageBitmap(imageBitmap)
        bindDiscardBtn()
        binding.imgPreview.visibility = View.VISIBLE
        binding.viewFinder.visibility = View.GONE
        imageUri = uri
    }

    private fun createImageFile(filename: String): File {
        return File(requireContext().cacheDir, filename)
    }

    private fun uploadPhotoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private val galleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    imageUri = uri
                    imageBitmap = decodeBitmapFromUri(uri)
                    updateImagePreview(uri)
                }
            }
        }

    private fun discardPhoto() {
        imageBitmap?.recycle()
        imageBitmap = null
        imageUri = null
        binding.imgPreview.visibility = View.GONE
        binding.viewFinder.visibility = View.VISIBLE
        binding.discardImgBtn.visibility = View.GONE
        startCamera()
    }

    private fun submitAttendance() {
        binding.btnSubmit.isEnabled = false

        if (imageBitmap == null || auth.currentUser?.uid == null) {
            showToast("No photo captured")
            return
        }

        val userId = auth.currentUser!!.uid
        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val filename =
            "${userId}_${currentDate}_${if (isCheckIn == true) "checkout" else "checkin"}.jpg"
        val storageRef = storage.reference.child("attendance/$filename")

        val data =
            ByteArrayOutputStream()
                .apply { imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 70, this) }
                .toByteArray()

        storageRef
            .putBytes(data)
            .addOnSuccessListener { saveAttendanceToDB(filename) }
            .addOnFailureListener {
                showToast("Upload failed")
                binding.btnSubmit.isEnabled = true
            }
    }

    private fun saveAttendanceToDB(filename: String) {
        val userId = auth.currentUser!!.uid
        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
        val attendanceItem =
            AttendanceItem(
                date = currentDate,
                checkInTime =
                    if (isCheckIn == true)
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    else "",
                checkOutPhotoUrl = if (isCheckIn == true) "" else filename)

        db.collection("attendance")
            .document(userId)
            .update("attendanceList", FieldValue.arrayUnion(attendanceItem))
            .addOnSuccessListener {
                showToast("Attendance submitted successfully")
                navigateBack()
            }
            .addOnFailureListener {
                showToast("Failed to save attendance")
                binding.btnSubmit.isEnabled = true
            }
    }

    private fun navigateBack() {
        findNavController().popBackStack()
    }

    private fun bindDiscardBtn() {
        binding.discardImgBtn.setOnClickListener { discardPhoto() }
        binding.discardImgBtn.visibility = View.VISIBLE
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
