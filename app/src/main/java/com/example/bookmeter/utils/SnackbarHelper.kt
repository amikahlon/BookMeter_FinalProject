package com.example.bookmeter.utils

import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.bookmeter.R
import com.google.android.material.snackbar.Snackbar

object SnackbarHelper {

    enum class SnackbarType {
        SUCCESS, ERROR, INFO, WARNING
    }

    fun showSnackbar(
        view: View,
        message: String,
        type: SnackbarType = SnackbarType.INFO,
        duration: Int = Snackbar.LENGTH_LONG,
        actionText: String = "OK",
        action: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(view, message, duration)

        // Apply styling based on notification type
        when (type) {
            SnackbarType.SUCCESS -> {
                snackbar.setBackgroundTint(ContextCompat.getColor(view.context, R.color.snackbar_success))
                snackbar.setActionTextColor(Color.WHITE)
            }
            SnackbarType.ERROR -> {
                snackbar.setBackgroundTint(ContextCompat.getColor(view.context, R.color.snackbar_error))
                snackbar.setActionTextColor(Color.WHITE)
            }
            SnackbarType.WARNING -> {
                snackbar.setBackgroundTint(ContextCompat.getColor(view.context, R.color.snackbar_warning))
                snackbar.setActionTextColor(Color.BLACK)
            }
            SnackbarType.INFO -> {
                snackbar.setBackgroundTint(ContextCompat.getColor(view.context, R.color.snackbar_info))
                snackbar.setActionTextColor(Color.WHITE)
            }
        }

        val snackbarView = snackbar.view
        val textView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        textView.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        textView.maxLines = 2

        snackbarView.elevation = 8f

        snackbarView.background = ContextCompat.getDrawable(view.context, R.drawable.snackbar_background)

        snackbar.setAction(actionText) {
            action?.invoke() ?: snackbar.dismiss()
        }

        snackbar.show()
    }


    fun showSuccess(view: View, message: String) {
        showSnackbar(view, message, SnackbarType.SUCCESS)
    }

    fun showError(view: View, message: String) {
        showSnackbar(view, message, SnackbarType.ERROR)
    }

    fun showInfo(view: View, message: String) {
        showSnackbar(view, message, SnackbarType.INFO)
    }

    fun showWarning(view: View, message: String) {
        showSnackbar(view, message, SnackbarType.WARNING)
    }
}