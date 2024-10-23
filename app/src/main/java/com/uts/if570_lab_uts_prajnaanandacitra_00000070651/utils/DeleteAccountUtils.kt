package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.utils

import androidx.fragment.app.Fragment
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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

    attendanceRef
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val attendance = document.toObject(Attendance::class.java)
                attendance?.attendanceList?.forEach { item ->
                    deleteImageFromStorage(item.checkInPhotoUrl, storage)
                    deleteImageFromStorage(item.checkOutPhotoUrl, storage)
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

private fun deleteImageFromStorage(imageUrl: String, storage: FirebaseStorage) {
    if (imageUrl.isNotEmpty()) {
        val imageRef = storage.getReferenceFromUrl(imageUrl)
        imageRef
            .delete()
            .addOnSuccessListener {
                // Image deleted successfully
            }
            .addOnFailureListener {
                // Handle failure to delete
            }
    }
}
