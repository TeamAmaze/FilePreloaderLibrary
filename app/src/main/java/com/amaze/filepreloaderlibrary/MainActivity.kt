package com.amaze.filepreloaderlibrary

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *              on 10/1/2018, at 15:04.
 */
class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission()
    }

    fun getFile():File {
        var externalDir: File = Environment.getExternalStorageDirectory() ?: throw IOException("Failed to read files")
        externalDir.setReadable(true)
        return externalDir
    }

    fun loadData() {
        val externalDir = getFile()
        FilePreloader.preloadFrom(externalDir.absolutePath, ::FileMetadata)
    }

    fun readData() {
        val externalDir = getFile()
        var metas: List<FileMetadata> = listOf()
        var str = ""

        val time = measureNanoTime {
            metas = FilePreloader.loadFrom(externalDir.absolutePath, ::FileMetadata)
        } / 1_000_000.0

        metas.map { str += "\n$it" }
        textView.text = "------------ FILES IN ${externalDir.absolutePath} ------------$str"

        timeView.text = "${time}ms"
    }

    fun onLoadButtonClick(v: View) = loadData()

    fun onReadButtonClick(v: View) = readData()

    fun onCleanUpClick(v: View) {
        Processor.cleanUp()
        textView.text = ""
        timeView.text = ""
    }

    fun checkPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                    0)
            return false
        }

        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 0 && grantResults.isNotEmpty() || grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            readData()
        } else {
            checkPermission()
        }
    }

}
