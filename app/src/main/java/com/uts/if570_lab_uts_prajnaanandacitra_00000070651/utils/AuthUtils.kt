package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.utils

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R

fun Fragment.sessionCheck() {
    val auth = Firebase.auth
    val currentUser = auth.currentUser
    val navController = findNavController()
    val currentDestination = navController.currentDestination?.id

    if (currentUser == null) {
        if (currentDestination != R.id.signInFragment) {
            navController.navigate(R.id.action_global_signInFragment)
        }
    } else {
        currentUser.getIdToken(true).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                if (currentUser.email == null) {
                    auth.signOut().let { navController.navigate(R.id.action_global_signInFragment) }
                } else {
                    if (currentDestination != R.id.mainFragment) {
                        navController.navigate(R.id.action_global_mainFragment)
                    }
                }
            } else {
                auth.signOut().let { navController.navigate(R.id.action_global_signInFragment) }
            }
        }
    }
}


