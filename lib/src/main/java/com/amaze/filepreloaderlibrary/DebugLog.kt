package com.amaze.filepreloaderlibrary

import android.util.Log

object DebugLog {
	fun log(tag: String, message: String) {
		if(FilePreloader.DEBUG) {
			Log.d(tag, message)
		}
	}

}