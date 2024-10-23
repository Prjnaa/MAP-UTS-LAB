package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.utils

import android.content.Context
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.auth
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.extensions.passwordVisiblityToggle

fun Fragment.showPassDialog(context: Context, onPasswordEntered: (String) -> Unit) {
    val dialogView = layoutInflater.inflate(R.layout.dialog_password_input, null)
    val passwordInput = dialogView.findViewById<EditText>(R.id.input_password)

    passwordInput.passwordVisiblityToggle(requireContext())

    val dialog =
        AlertDialog.Builder(context)
            .setTitle("Re-Authenticate")
            .setView(dialogView)
            .setPositiveButton("Confirm") { _, _ ->
                val password = passwordInput.text.toString()
                if (password.isNotEmpty()) {
                    onPasswordEntered(password)
                } else {
                    Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

    dialog.show()

    dialog.window?.setBackgroundDrawable(context.getDrawable(R.drawable.dialog_bg))
    dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(context.getColor(R.color.safe))
    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(context.getColor(R.color.danger))
}

fun Fragment.showChangePassDialog(context: Context) {
    val changePassDialog = layoutInflater.inflate(R.layout.dialog_password_reset, null)
    val currentPassInput = changePassDialog.findViewById<EditText>(R.id.current_pass_input)
    val newPassInput = changePassDialog.findViewById<EditText>(R.id.new_pass_input)
    val confirmPassInput = changePassDialog.findViewById<EditText>(R.id.new_pass_confirm_input)

    val user = Firebase.auth.currentUser

    confirmPassInput.passwordVisiblityToggle(requireContext())
    newPassInput.passwordVisiblityToggle(requireContext())
    currentPassInput.passwordVisiblityToggle(requireContext())

    val resetPassDialog =
        AlertDialog.Builder(context)
            .setTitle("Reset Password")
            .setView(changePassDialog)
            .setPositiveButton("Confirm") { _, _ ->
                user?.let {
                    val email = user.email

                    val currentPass = currentPassInput.text.toString()

                    val credentials = EmailAuthProvider.getCredential(email!!, currentPass)

                    if (currentPass.isNotEmpty()) {
                        user.reauthenticate(credentials).addOnSuccessListener {
                            val newPass = newPassInput.text.toString()
                            val confirmPass = confirmPassInput.text.toString()

                            if (newPass.isNotEmpty() && confirmPass.isNotEmpty()) {
                                if (newPass == confirmPass) {
                                    user!!
                                        .updatePassword(newPass)
                                        .addOnSuccessListener {
                                            Toast.makeText(
                                                    context,
                                                    "New password has been set",
                                                    Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(
                                                    context,
                                                    "Failed to set new password",
                                                    Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                } else {
                                    newPassInput.error = "Password does not match"
                                    confirmPassInput.error = "Password does not match"
                                }
                            } else {
                                newPassInput.error = "Password cannot be empty"
                                confirmPassInput.error = "Password cannot be empty"
                            }
                        }
                    } else {
                        currentPassInput.error = "Please fill your current password"
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

    resetPassDialog.show()

    resetPassDialog.window?.setBackgroundDrawable(context.getDrawable(R.drawable.dialog_bg))

    resetPassDialog
        .getButton(AlertDialog.BUTTON_POSITIVE)
        ?.setTextColor(context.getColor(R.color.safe))
    resetPassDialog
        .getButton(AlertDialog.BUTTON_NEGATIVE)
        ?.setTextColor(context.getColor(R.color.danger))
}
