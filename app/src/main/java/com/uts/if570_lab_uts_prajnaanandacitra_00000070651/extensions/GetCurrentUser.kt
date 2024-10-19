package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.extensions

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.db.models.User

fun FirebaseAuth.getCurrentUserFromDB(db: FirebaseFirestore, onUserFetched: (User?) -> Unit) {
    currentUser?.let { user ->
        val uid = user.uid
        val userRef = db.collection("users").document(uid)

        userRef
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username") ?: ""
                    val email = document.getString("email") ?: ""
                    val imageUrl = document.getString("imageUrl") ?: ""

                    val userInfo = User(username, email, imageUrl)

                    onUserFetched(userInfo)
                } else {
                    onUserFetched(null)
                }
            }
            .addOnFailureListener { onUserFetched(null) }
    } ?: run { onUserFetched(null) }
}
