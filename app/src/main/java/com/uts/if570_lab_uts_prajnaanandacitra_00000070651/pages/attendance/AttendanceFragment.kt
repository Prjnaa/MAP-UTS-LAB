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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.databinding.FragmentAttendanceBinding
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.config.FirebaseConfig
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.db.models.Attendance
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.db.models.AttendanceItem
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.utils.sessionCheck
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

        sessionCheck()

        // Initialize Firebase instances
        auth = Firebase.auth
        db = FirebaseConfig.getFirestore()
        storage = FirebaseConfig.getStorage()

        // Handle camera permissions
        handleCameraPermission()

        getIsUserCheckedInFromDB { checkInState -> isCheckIn = checkInState }


        with(binding) {
            if(isCheckIn == true) {
                btnSubmit.text = getString(R.string.submit_in)
            } else {
                btnSubmit.text = getString(R.string.submit_out)
            }

            discardImgBtn.visibility = View.GONE

            btnCapture.setOnClickListener { takePhoto() }

            backButtonFromAttendance.setOnClickListener {
                stopCamera()
                imageCapture = null
                findNavController().navigate(R.id.action_attendanceFragment_to_mainFragment)
            }

            btnUpload.setOnClickListener { uploadPhotoFromGallery() }

            btnSubmit.setOnClickListener { submitAttendance() }
        }
    }

    private fun getIsUserCheckedInFromDB(callback: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val userRef = db.collection("attendance").document(uid)

        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        userRef
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val attendance = document.toObject(Attendance::class.java)
                    attendance?.attendanceList?.let { attendanceList ->
                        val todayAttendance =
                            attendanceList.find { item -> item.date == currentDate }
                        if (todayAttendance != null) {
                            // User has an attendance record for today
                            isCheckIn = todayAttendance.checkInTime.isNotEmpty()
                        } else {
                            // No attendance record for today
                            isCheckIn = false
                        }
                        callback(isCheckIn!!)
                    }
                        ?: run {
                            isCheckIn = false
                            callback(false)
                        }
                } else {
                    isCheckIn = false
                    callback(false)
                }
            }
            .addOnFailureListener {
                isCheckIn = false
                callback(false)
            }
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
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean
            ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    // Start camera preview
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())

        cameraProviderFuture.addListener(
            {
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview =
                    Preview.Builder().build().also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }

                imageCapture = ImageCapture.Builder().build()

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
                } catch (ex: Exception) {
                    Toast.makeText(requireContext(), ex.message, Toast.LENGTH_SHORT).show()
                }
            },
            ContextCompat.getMainExecutor(requireContext()))
    }

    // Capture a photo
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val uid = auth.currentUser?.uid ?: "unknown_user"
        val dateFormat = SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault())
        val currentDateTime = dateFormat.format(System.currentTimeMillis())
        val attendanceType = if (isCheckIn == true) "check_in" else "check_out"

        val filename = "${uid}_${currentDateTime}_${attendanceType}.jpg"
        val outputOptions =
            ImageCapture.OutputFileOptions.Builder(createImageFile(filename)).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val uri = outputFileResults.savedUri
                    uri?.let {
                        imageBitmap =
                            if (android.os.Build.VERSION.SDK_INT >=
                                android.os.Build.VERSION_CODES.P) {
                                ImageDecoder.decodeBitmap(
                                    ImageDecoder.createSource(requireContext().contentResolver, it))
                            } else {
                                MediaStore.Images.Media.getBitmap(
                                    requireContext().contentResolver, it)
                            }

                        //                        imageBitmap = correctOrientation(imageBitmap!!,
                        // it)

                        binding.imgPreview.setImageBitmap(imageBitmap)
                        bindDiscardBtn()
                        binding.imgPreview.visibility = View.VISIBLE
                        binding.viewFinder.visibility = View.GONE
                        imageUri = uri
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(requireContext(), "Photo capture failed", Toast.LENGTH_SHORT)
                        .show()
                }
            })
    }

    // Rotate the bitmap if needed based on the image's EXIF orientation
    private fun correctOrientation(bitmap: Bitmap, uri: Uri): Bitmap {
        val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return bitmap
        val exifInterface = androidx.exifinterface.media.ExifInterface(inputStream)
        val orientation =
            exifInterface.getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL)
        inputStream.close() // Close stream after use
        return when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 ->
                rotateImage(bitmap, 90f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 ->
                rotateImage(bitmap, 180f)
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 ->
                rotateImage(bitmap, 270f)
            else -> bitmap
        }
    }

    // Helper function to rotate the image
    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    // Create a file for the image
    private fun createImageFile(filename: String): File {
        val storageDir = requireContext().cacheDir
        return File(storageDir, filename)
    }

    //    upload photo from gallery
    private fun uploadPhotoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private val galleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri: Uri ->
                    imageUri = uri

                    imageBitmap =
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            ImageDecoder.decodeBitmap(
                                ImageDecoder.createSource(requireContext().contentResolver, uri))
                        } else {
                            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
                        }

                    //                    imageBitmap = correctOrientation(imageBitmap!!, uri)

                    binding.imgPreview.setImageBitmap(imageBitmap)
                    bindDiscardBtn()
                    binding.imgPreview.visibility = View.VISIBLE
                    binding.viewFinder.visibility = View.GONE
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

    // Submit attendance
    private fun submitAttendance() {
        binding.btnSubmit.isEnabled = false

        if (imageBitmap == null || auth.currentUser?.uid == null) {
            Toast.makeText(requireContext(), "No photo captured", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid!!
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val filename =
            "${userId}_${currentDate}_${if (isCheckIn == true) "checkout" else "checkin"}.jpg"
        val storageRef = storage.reference.child("attendance/$filename")

        val baos = ByteArrayOutputStream()
        imageBitmap?.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val data = baos.toByteArray()

        storageRef
            .putBytes(data)
            .addOnSuccessListener {
                saveAttendanceToFirestore(filename)
                isCheckIn = !isCheckIn!!
                stopCamera()
                findNavController().navigate(R.id.action_attendanceFragment_to_mainFragment)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Upload failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveAttendanceToFirestore(filename: String) {
        val userId = auth.currentUser?.uid ?: return
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

        val attendanceRef = db.collection("attendance").document(userId)

        attendanceRef
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val attendance = document.toObject(Attendance::class.java)
                    attendance?.let {
                        val updatedAttendanceList = it.attendanceList.toMutableList()
                        val todayAttendanceIndex =
                            updatedAttendanceList.indexOfFirst { item -> item.date == currentDate }

                        if (todayAttendanceIndex != -1) {
                            // Update existing attendance for today
                            val updatedItem =
                                updatedAttendanceList[todayAttendanceIndex].copy(
                                    checkInTime =
                                        if (isCheckIn == true) currentTime
                                        else
                                            updatedAttendanceList[todayAttendanceIndex].checkInTime,
                                    checkOutTime =
                                        if (isCheckIn == false) currentTime
                                        else
                                            updatedAttendanceList[todayAttendanceIndex]
                                                .checkOutTime,
                                    checkInPhotoUrl =
                                        if (isCheckIn == true) filename
                                        else
                                            updatedAttendanceList[todayAttendanceIndex]
                                                .checkInPhotoUrl,
                                    checkOutPhotoUrl =
                                        if (isCheckIn == false) filename
                                        else
                                            updatedAttendanceList[todayAttendanceIndex]
                                                .checkOutPhotoUrl)
                            updatedAttendanceList[todayAttendanceIndex] = updatedItem
                        } else {
                            // Create new attendance for today
                            val newAttendanceItem =
                                AttendanceItem(
                                    date = currentDate,
                                    checkInTime = if (isCheckIn == true) currentTime else "",
                                    checkOutTime = if (isCheckIn == false) currentTime else "",
                                    checkInPhotoUrl = if (isCheckIn == true) filename else "",
                                    checkOutPhotoUrl = if (isCheckIn == false) filename else "")
                            updatedAttendanceList.add(newAttendanceItem)
                        }

                        attendanceRef
                            .update("attendanceList", updatedAttendanceList)
                            .addOnSuccessListener {
                                Toast.makeText(
                                        requireContext(), "Attendance updated", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT)
                                    .show()
                            }
                    }
                } else {
                    // Document does not exist, create a new one
                    val newAttendanceItem =
                        AttendanceItem(
                            date = currentDate,
                            checkInTime = if (isCheckIn == false) currentTime else "",
                            checkOutTime = if (isCheckIn == true) currentTime else "",
                            checkInPhotoUrl = if (isCheckIn == false) filename else "",
                            checkOutPhotoUrl = if (isCheckIn == true) filename else "")
                    val newAttendance =
                        Attendance(userId = userId, attendanceList = listOf(newAttendanceItem))

                    attendanceRef
                        .set(newAttendance)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Attendance saved", Toast.LENGTH_SHORT)
                                .show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), e.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun stopCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())

        cameraProviderFuture.addListener(
            { cameraProviderFuture.get().unbindAll() },
            ContextCompat.getMainExecutor(requireContext()))
    }

    private fun bindDiscardBtn() {
        binding.discardImgBtn.visibility = View.VISIBLE
        binding.discardImgBtn.setOnClickListener { discardPhoto() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCamera()
        imageCapture = null
        _binding = null
    }
}
