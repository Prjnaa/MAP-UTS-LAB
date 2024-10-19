package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.utils

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R

fun Fragment.sessionCheck() {
    val currentUser = FirebaseAuth.getInstance().currentUser
    if (currentUser == null) {
        findNavController().navigate(R.id.action_global_signInFragment)
    }
}