package com.amaze.filepreloaderlibrary.sampleapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.*
import com.amaze.filepreloaderlibrary.FilePreloader
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback,
        AdapterView.OnItemClickListener {

    private val pathList: MutableList<String?> = mutableListOf()

    private var adapter: ArrayAdapter<String>? = null
    private var currentPath = ""
    private var showingLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermission()

        FilePreloader.DEBUG = true

        val fileList = findViewById<ListView>(R.id.filelist)

        adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mutableListOf())
        fileList.adapter = adapter
        fileList.onItemClickListener = this
        loadFolder(getStartingFile().absolutePath)
    }

    override fun onBackPressed() {
        loadFolder(getStartingFile().absolutePath)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if(!showingLoaded) pathList[position]?.let { loadFolder(it) }
    }

    private fun loadFolder(path: String) {
        FilePreloader.with(::FileMetadata).preloadFrom(path)//PRELOAD NEXT POSSIBLE FOLDERS

        val adapter = this.adapter ?: throw NullPointerException()
        val externalDir = File(path)
        val fileList = mutableListOf<String>()

        val timeStart = System.nanoTime()
        FilePreloader.with(::FileMetadata).load(this, path) {
            val timeDelta = (System.nanoTime() - timeStart)/ 1_000_000.0

            currentPath = externalDir.absolutePath

            pathList.clear()
            pathList.addAll(mutableListOf(path, externalDir.parent))

            adapter.clear()
            adapter.addAll(listOf(".", ".."))

            it.forEach {
                fileList.add(it.toString())
                pathList.add(if(it.isDirectory) it.path else null)
            }

            adapter.addAll(fileList)

            val timeView = findViewById<TextView>(R.id.timeView)
            timeView.text = "${timeDelta}ms\n---- FILES IN ${externalDir.absolutePath} (${it.size}) ----"
        }
    }



    private fun getStartingFile(): File {
        lateinit var externalDir: File

        if(isEmulator()) {
            externalDir = File("/sdcard/")
        } else {
            val externalFile = Environment.getExternalStorageDirectory() ?: throw IOException("Failed to read files")
            externalDir = File(externalFile.path)
        }

        externalDir.setReadable(true)
        return externalDir
    }

    private fun dump() {
        val adapter = this.adapter ?: throw NullPointerException()

        val timeStart = System.nanoTime()

        FilePreloader.getAllDataLoaded(this) {
            val timeDelta = (System.nanoTime() - timeStart)/ 1_000_000.0

            adapter.clear()
            val a: List<String>? = it?.map { it.toString() }
            if(a != null) adapter.addAll(a)

            val timeView = findViewById<TextView>(R.id.timeView)
            timeView.text = "${timeDelta}ms\n------ FILES DUMP FOR ${currentPath} ------"
        }
    }

    fun onReadButtonClick(v: View) {
        val loadedButton = findViewById<Button>(R.id.loadedButton)

        if(!showingLoaded) {
            dump()
            loadedButton.text = "HIDE LOADED"
        } else  {
            loadFolder(currentPath)
            loadedButton.text = "SHOW LOADED"
        }

        showingLoaded = !showingLoaded
    }

    fun onCleanUpClick(v: View) {
        FilePreloader.cleanUp()
        if(showingLoaded) adapter?.clear()
        val timeView = findViewById<TextView>(R.id.timeView)
        timeView.text = ""
    }

    private fun checkPermission(): Boolean {
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
            dump()
        } else {
            checkPermission()
        }
    }

}
