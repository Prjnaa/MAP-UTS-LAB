package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.utils

import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.db.models.Attendance

fun Fragment.deleteAccount(
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    storage: FirebaseStorage,
    onComplete: (Boolean) -> Unit
) {
    val user = auth.currentUser ?: return onComplete(false)

    showPassDialog(requireContext()) { password ->
        val userEmail = user.email
        if (userEmail != null) {
            val credentials = EmailAuthProvider.getCredential(userEmail, password)
            user
                .reauthenticate(credentials)
                .addOnSuccessListener {
                    deleteUserData(user.uid, db, storage) { success ->
                        if (success) {
                            user
                                .delete()
                                .addOnSuccessListener {
                                    auth.signOut()
                                    onComplete(true)
                                }
                                .addOnFailureListener { onComplete(false) }
                        } else {
                            onComplete(false)
                        }
                    }
                }
                .addOnFailureListener { onComplete(false) }
        }
    }
}

private fun deleteUserData(
    userId: String,
    db: FirebaseFirestore,
    storage: FirebaseStorage,
    onComplete: (Boolean) -> Unit
) {
    val userDocRef = db.collection("users").document(userId)
    val attendanceRef = db.collection("attendance").document(userId)

    userDocRef.get().addOnSuccessListener { user ->
        if (user.exists()) {
            val userImageUrl = user.getString("imageUrl")
            userImageUrl?.let { url -> deleteImageFromStorage(url, storage, "user") }
        }
    }

    attendanceRef
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val attendance = document.toObject(Attendance::class.java)
                attendance?.attendanceList?.forEach { item ->
                    deleteImageFromStorage(item.checkInPhotoUrl, storage, "attendance")
                    deleteImageFromStorage(item.checkOutPhotoUrl, storage, "attendance")
                }
            }

            userDocRef
                .delete()
                .addOnSuccessListener {
                    attendanceRef
                        .delete()
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                }
                .addOnFailureListener { onComplete(false) }
        }
        .addOnFailureListener { onComplete(false) }
}

private fun deleteImageFromStorage(imageFileName: String, storage: FirebaseStorage, path: String) {
    if (imageFileName.isNotEmpty()) {

        val storageAttendancePath = "gs://uts-map-53224.appspot.com/attendance/$imageFileName"
        val storageUserPath = "gs://uts-map-53224.appspot.com/image/$imageFileName"

        var imageRef: StorageReference
        try {
            imageRef =
                if (path == "attendance") {
                    storage.getReferenceFromUrl(storageAttendancePath)
                } else if (path == "user") {
                    storage.getReferenceFromUrl(storageUserPath)
                } else {
                    throw IllegalArgumentException("Invalid path provided")
                }

            imageRef.delete()

        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }
    }
}
