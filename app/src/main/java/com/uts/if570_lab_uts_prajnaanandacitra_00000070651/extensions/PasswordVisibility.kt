package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R

fun EditText.passwordVisiblityToggle(context: Context) {
    val eyeOpenIcon: Drawable? =
        ContextCompat.getDrawable(context, R.drawable.baseline_visibility_24)
    val eyeShutIcon: Drawable? =
        ContextCompat.getDrawable(context, R.drawable.baseline_visibility_off_24)

    setCompoundDrawablesWithIntrinsicBounds(null, null, eyeOpenIcon, null)

    var isVisible = false

    setOnTouchListener { v, event ->
        if(event.action == MotionEvent.ACTION_UP) {
            val drawableEnd = 2
            if(event.rawX >= (this.right - this.compoundDrawables[drawableEnd].bounds.width())) {
                isVisible = !isVisible

                if(isVisible) {
                    this.transformationMethod = HideReturnsTransformationMethod.getInstance()
                    setCompoundDrawablesWithIntrinsicBounds(null, null, eyeShutIcon, null)
                } else {
                    this.transformationMethod = PasswordTransformationMethod.getInstance()
                    setCompoundDrawablesWithIntrinsicBounds(null, null, eyeOpenIcon, null)
                }

                this.setSelection(this.text.length)
                v.performClick()
                return@setOnTouchListener true
            }
        }
        false
    }
}