package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.config

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseConfig {
    lateinit var firestoreInstance: FirebaseFirestore

    fun initializeApp(context: Context) {
        FirebaseApp.initializeApp(context)

        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        initializeDb()
    }

    fun initializeDb() {
        firestoreInstance = FirebaseFirestore.getInstance()
    }
}