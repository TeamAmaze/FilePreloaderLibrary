package com.amaze.filepreloaderlibrary.utils

import android.util.Log
import com.amaze.filepreloaderlibrary.FilePreloader

object DebugLog {
	fun log(tag: String, message: String) {
		if(FilePreloader.DEBUG) {
			Log.d(tag, message)
		}
	}

}