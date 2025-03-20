package com.example.bookmeter.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Helper class for handling runtime permissions
 */
object PermissionHelper {
    
    // Permission constants
    const val READ_EXTERNAL_STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
    
    /**
     * Check if the app has storage permission
     * @param context Application context
     * @return Boolean indicating if permission is granted
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+, we check for the specific media permissions
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For older versions, we use the storage permission
            ContextCompat.checkSelfPermission(
                context,
                READ_EXTERNAL_STORAGE_PERMISSION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get the appropriate storage permission based on Android version
     * @return The permission string to request
     */
    fun getStoragePermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            READ_EXTERNAL_STORAGE_PERMISSION
        }
    }
    
    /**
     * Creates an intent to open app settings
     */
    fun createAppSettingsIntent(context: Context): Intent {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", context.packageName, null)
        intent.data = uri
        return intent
    }
}
