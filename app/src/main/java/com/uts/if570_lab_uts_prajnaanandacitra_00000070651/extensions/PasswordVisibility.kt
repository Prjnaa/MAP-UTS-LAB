package com.uts.if570_lab_uts_prajnaanandacitra_00000070651.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.MotionEvent
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.uts.if570_lab_uts_prajnaanandacitra_00000070651.R

fun EditText.passwordVisibilityToggle(context: Context) {
    val eyeOpenIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.baseline_visibility_24)
    val eyeShutIcon: Drawable? = ContextCompat.getDrawable(context, R.drawable.baseline_visibility_off_24)

    var isVisible = false
    updateIcon(isVisible, eyeOpenIcon, eyeShutIcon)

    setOnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_UP && isTouchingDrawableEnd(event)) {
            isVisible = !isVisible
            updateVisibility(isVisible, eyeOpenIcon, eyeShutIcon)
            v.performClick()
            true
        } else {
            false
        }
    }
}

private fun EditText.isTouchingDrawableEnd(event: MotionEvent): Boolean {
    val drawableEnd = 2
    return event.rawX >= (this.right - this.compoundDrawables[drawableEnd]?.bounds?.width()!! ?: 0)
}

private fun EditText.updateVisibility(isVisible: Boolean, eyeOpenIcon: Drawable?, eyeShutIcon: Drawable?) {
    transformationMethod = if (isVisible) {
        HideReturnsTransformationMethod.getInstance()
    } else {
        PasswordTransformationMethod.getInstance()
    }
    updateIcon(isVisible, eyeOpenIcon, eyeShutIcon)
    setSelection(text.length)
}

private fun EditText.updateIcon(isVisible: Boolean, eyeOpenIcon: Drawable?, eyeShutIcon: Drawable?) {
    val icon = if (isVisible) eyeShutIcon else eyeOpenIcon
    setCompoundDrawablesWithIntrinsicBounds(null, null, icon, null)
}
