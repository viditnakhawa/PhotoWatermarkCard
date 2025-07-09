package com.viditnakhawa.photowatermarkcard.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.OutputStream
import android.net.Uri
import androidx.core.graphics.createBitmap

fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
    val filename = "photo_card_${System.currentTimeMillis()}.png"
    val fos: OutputStream?
    val imageUri: Uri?

    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
    imageUri = uri
    fos = uri?.let { resolver.openOutputStream(it) }

    fos?.use {
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        Toast.makeText(context, "Image saved to gallery!", Toast.LENGTH_SHORT).show()
    }

    imageUri?.let {
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, it))
    }
}


fun captureComposableToBitmap(view: View): Bitmap {
    val bitmap = createBitmap(view.width, view.height)
    val canvas = Canvas(bitmap)
    view.draw(canvas)
    return bitmap
}
