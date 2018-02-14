package com.amaze.filepreloaderlibrary

import android.util.Log
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import java.io.File
import java.io.FileFilter
import java.util.*
import kotlin.concurrent.thread

/**
 * Basically means call `[ProcessUnit].second` on each of `[ProcessUnit].first`'s files.
 * This is done asynchly.
 *
 * @see Processor.workFrom
 * @see Processor.work
 */
typealias ProcessUnit = Pair<String, (String) -> DataContainer>

/**
 * Contains the data (`[ProcessedUnit].second`) for a file inside the path `[ProcessedUnit].first`
 */
typealias ProcessedUnit = Pair<String, DataContainer>

const val DIVIDER = "/"

/**
 * Thread safe.
 * All the callable executions to load all the folders.
 *
 * 'Load a folder' means that the function `[unit].second` will be called
 * on each file (represented by its path) inside the folder.
 */
private val PRELOAD_LIST: MutableList<Loader> = Collections.synchronizedList(mutableListOf<Loader>())

/**
 * Thread safe.
 * Maps each folder to a list of [DataContainer] with each file's data.
 * The first folder is passed in [Processor.workFrom] or [Processor.work] to get the result of the
 * load folder operation.
 *
 * 'Load a folder' means that the function `[unit].second` will be called
 * on each file (represented by its path) inside the folder.
 */
private val PRELOADED_MAP: MutableMap<String, PreloadedFolder> =
        Collections.synchronizedMap(hashMapOf<String, PreloadedFolder>())

/**
 * Singleton charged with writing to [PRELOAD_LIST] and starting the preload
 * and, afterwards reading from [PRELOADED_MAP] and returning the output.
 */
object Processor {
    /**
     * Asynchly load every folder inside the path `[unit].first` except itself (aka '.').
     * It will even load the parent (aka '..'), as this method implies that the user can go up
     * (in the filesystem tree).
     *
     * 'Load a folder' means that the function `[unit].second` will be called
     * on each file (represented by its path) inside the folder.
     */
    fun workFrom(unit: ProcessUnit) {
        launch {
            synchronized(PRELOADED_MAP) {
                val file = File(unit.first)
                (file.listFiles(FileFilter { it.isDirectory }) as Array<File>?)
                        ?.forEach {
                            if(PRELOADED_MAP[it.path] == null) {
                                val subfiles = it.list()
                                for (filename in subfiles) {
                                    Processor.addToProcess(it.path, ProcessUnit(it.absolutePath + DIVIDER + filename, unit.second))
                                }

                                PRELOADED_MAP[it.path] = PreloadedFolder(subfiles.size)
                            }
                        }

                if(PRELOADED_MAP[file.parent] == null) {
                    val parentFileList: Array<File>? = file.parentFile.listFiles()
                    if (parentFileList != null && PRELOADED_MAP[file.parent] == null) {
                        parentFileList.forEach {
                            Processor.addToProcess(file.parent, ProcessUnit(it.path, unit.second))
                        }
                        PRELOADED_MAP[file.parent] = PreloadedFolder(parentFileList.size)
                    }
                }
            }

            work()
        }
    }

    /**
     * Asynchly loads the folder in `[unit].first`.
     *
     * 'Load a folder' means that the function `[unit].second` will be called
     * on each file (represented by its path) inside the folder.
     */
    fun work(unit: ProcessUnit) {
        launch {
            synchronized(PRELOADED_MAP) {
                val file = File(unit.first)
                val fileList = file.list()
                for (path in fileList) {
                    Processor.addToProcess(file.path, ProcessUnit(file.absolutePath + DIVIDER + path, unit.second))
                }
                PRELOADED_MAP[file.path] = PreloadedFolder(fileList.size)
            }
            work()
        }
    }

    /**
     * Clear everything, all data loaded will be discarded.
     */
    fun cleanUp() {
        PRELOAD_LIST.clear()
        PRELOADED_MAP.clear()
    }

    /**
     * Add file (represented by [unit]) to the [PRELOAD_LIST] to be preloaded by [Threader].
     */
    private fun addToProcess(path: String, unit: ProcessUnit) {
        PRELOAD_LIST.add(Loader(path, unit))
    }

    /**
     * Gets the loaded files from the folder denoted by [path].
     *
     * @see work to understand.
     */
    fun getLoaded(path: String): Pair<Boolean, List<DataContainer>>? {
        synchronized(PRELOADED_MAP) {
            val completeSet = PRELOADED_MAP[path]
            if (completeSet == null) return null
            else return completeSet.isComplete() to completeSet.toList()
        }
    }

    /**
     * *This function is only to test what data is being preloaded.*
     * Gets all the loaded files inside the folders (except '.') in the folder denoted by the [path].
     * The parameter [clean] indicates if the data should be cleaned from the library's memory.
     *
     * @see workFrom to understand.
     */
    fun getAllData(): List<DataContainer>? {
        synchronized(PRELOADED_MAP) {
            val completeList = mutableListOf<DataContainer>()
            PRELOADED_MAP.map { completeList.addAll(it.value) }
            return completeList
        }
    }
}

/**
 * NEVER CALL ON MAIN THREAD
 * Loads every element in PRELOAD_LIST
 */
fun work() {
    synchronized(PRELOAD_LIST) {
        PRELOAD_LIST.forEach {
            val (path, data) = it.load()

            Log.d("Threader", "Done: ($path, $data)")
            PRELOADED_MAP[path]!!.add(data)
        }
    }
}

/**
 * Each [Loader] contains en element whose metadata it will get.
 * Basically runs `[unit].second.invoke([unit].first)` and saves the result.
 */
class Loader(private val path:String, private val unit:ProcessUnit) {
    fun load(): ProcessedUnit {
        return ProcessedUnit(path, unit.second.invoke(unit.first))
    }
}