package com.amaze.filepreloaderlibrary

object Native {
	init {
		System.loadLibrary("native-filesystem-functions")
	}

	external fun helloWorld(): String
}