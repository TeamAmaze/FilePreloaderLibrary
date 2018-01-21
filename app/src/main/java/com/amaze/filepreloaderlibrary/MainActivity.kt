package com.amaze.filepreloaderlibrary

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import kotlin.system.measureNanoTime

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *              on 10/1/2018, at 15:04.
 */
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

        adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mutableListOf())
        filelist.adapter = adapter
        filelist.onItemClickListener = this
        loadFolder(getStartingFile().absolutePath)
    }

    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if(!showingLoaded) pathList[position]?.let { loadFolder(it) }
    }

    private fun loadFolder(path: String) {
        FilePreloader.preloadFrom(path, ::FileMetadata)//PRELOAD NEXT POSSIBLE FOLDERS

        val adapter = this.adapter ?: throw NullPointerException()
        val externalDir = File(path)
        val fileList = mutableListOf<String>()
        lateinit var metas: List<FileMetadata>
        val time = measureNanoTime { metas = FilePreloader.load(path, ::FileMetadata) } / 1_000_000.0

        currentPath = externalDir.absolutePath

        pathList.clear()
        pathList.addAll(mutableListOf(path, externalDir.parent))

        adapter.clear()
        adapter.addAll(listOf(".", ".."))

        metas.forEach {
            fileList.add(it.toString())
            pathList.add(if(it.isDirectory) it.path else null)
        }

        adapter.addAll(fileList)

        timeView.text = "${time}ms\n------------ FILES IN ${externalDir.absolutePath} ------------"
    }

    private fun getStartingFile():File {
        var externalDir: File = Environment.getExternalStorageDirectory() ?: throw IOException("Failed to read files")
        externalDir = File(externalDir.path)
        externalDir.setReadable(true)
        return externalDir
    }

    private fun dump() {
        val adapter = this.adapter ?: throw NullPointerException()

        var metas: List<FileMetadata>? = null

        val time = measureNanoTime {
            metas = FilePreloader.getAllDataLoaded()
        } / 1_000_000.0

        adapter.clear()
        adapter.addAll(metas?.map { it.toString() })

        timeView.text = "${time}ms\n------ FILES DUMP FOR ${currentPath} ------"
    }

    fun onReadButtonClick(v: View) {
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
        Processor.cleanUp()
        if(showingLoaded) adapter?.clear()
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
