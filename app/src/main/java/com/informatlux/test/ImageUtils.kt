package com.informatlux.test

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.InputStream

object ImageUtils {
    private const val TAG = "ImageUtils"
    
    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bitmap from URI: $uri", e)
            null
        }
    }
    
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()
        
        var finalWidth = maxWidth
        var finalHeight = maxHeight
        
        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }
        
        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }
}