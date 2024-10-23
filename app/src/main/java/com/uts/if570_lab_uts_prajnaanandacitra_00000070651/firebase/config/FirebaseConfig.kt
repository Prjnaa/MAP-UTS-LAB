package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.firebase.config

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

object FirebaseConfig {
    @Volatile private var firestoreInstance: FirebaseFirestore? = null

    @Volatile private var storageInstance: FirebaseStorage? = null

    fun initializeApp(context: Context) {
        try {
            FirebaseApp.initializeApp(context)

            FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())

            FirebaseAppCheck.getInstance()
                .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())

            initializeDb()
            initializeStorage()
        } catch (e: Exception) {
            Log.e("FirebaseError", "Firebase initialization error: ${e.message}", e)
        }
    }

    private fun initializeDb() {
        if (firestoreInstance == null) {
            synchronized(this) {
                if (firestoreInstance == null) {
                    firestoreInstance = FirebaseFirestore.getInstance()
                }
            }
        }
    }

    private fun initializeStorage() {
        if (storageInstance == null) {
            synchronized(this) {
                if (storageInstance == null) {
                    storageInstance = FirebaseStorage.getInstance()
                }
            }
        }
    }

    fun getFirestore(): FirebaseFirestore {
        return firestoreInstance ?: throw IllegalStateException("Firestore not initialized")
    }

    fun getStorage(): FirebaseStorage {
        return storageInstance ?: throw IllegalStateException("Storage not initialized")
    }
}
