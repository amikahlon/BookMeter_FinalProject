package com.example.bookmeter.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionHelper {
    // Permission constant
    const val READ_EXTERNAL_STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
    
    /**
     * Checks if storage permission is granted
     */
    fun hasStoragePermission(context: Context): Boolean {
        // On Android 13+ we don't need this permission for picking single images
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        
        return ContextCompat.checkSelfPermission(
            context,
            READ_EXTERNAL_STORAGE_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
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
