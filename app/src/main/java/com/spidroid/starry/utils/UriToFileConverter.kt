package com.spidroid.starry.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object UriToFileConverter {
    private const val TAG = "UriToFileConverter"

    fun toFile(context: Context, uri: Uri?): File? {
        if (uri == null) return null

        var file: File? = null
        val fileName = UriToFileConverter.getFileName(context, uri)

        if (fileName != null) {
            try {
                val inputStream = context.getContentResolver().openInputStream(uri)
                if (inputStream != null) {
                    file = File(context.getCacheDir(), fileName)
                    val outputStream = FileOutputStream(file)
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.close()
                    inputStream.close()
                    return file
                }
            } catch (e: Exception) {
                Log.e(UriToFileConverter.TAG, "خطأ في تحويل URI إلى ملف: " + e.message, e)
            }
        }
        return null
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.getScheme() == "content") {
            context.getContentResolver().query(uri, null, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath()
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }
}