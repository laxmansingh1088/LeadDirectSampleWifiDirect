package com.example.leadp2pdirect.helpers

import android.content.Context
import android.widget.Toast
import java.io.File

object Utils {
    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    // For to Delete the directory inside list of files and inner Directory
    fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory()) {
            val children: Array<String> = dir.list()
            for (i in children.indices) {
                val success = deleteDir(File(dir, children[i]))
                if (!success) {
                    return false
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete()
    }
}