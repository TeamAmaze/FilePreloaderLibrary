package com.amaze.filepreloaderlibrary

import com.amaze.filepreloaderlibrary.Processor.PRELOADED_MAP
import com.amaze.filepreloaderlibrary.Processor.PRELOAD_LIST
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import java.io.FileFilter
import java.util.*

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

/**
 * Singleton charged with writing to [PRELOAD_LIST] and starting the preload
 * and, afterwards reading from [PRELOADED_MAP] and returning the output.
 */
object Processor {
    /**
     * Thread safe.
     * All the callable executions to load all the folders.
     *
     * 'Load a folder' means that the function `[unit].second` will be called
     * on each file (represented by its path) inside the folder.
     */
    private val PRELOAD_LIST: MutableList<() -> ProcessedUnit> =
            Collections.synchronizedList(mutableListOf<() -> ProcessedUnit>())
    private val PRELOAD_LIST_MUTEX = Mutex()

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
    private val PRELOADED_MAP_MUTEX = Mutex()

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
            val file = KFile(unit.first)

            //Load current folder
            PRELOADED_MAP_MUTEX.withLock {
                if (PRELOADED_MAP[file.path] == null) {
                    val subfiles: Array<String> = file.list() ?: arrayOf()
                    for (filename in subfiles) {
                        Processor.addToProcess(file.path, ProcessUnit(file.path + DIVIDER + filename, unit.second))
                    }

                    PRELOADED_MAP[file.path] = PreloadedFolder(subfiles.size)
                }
            }

            //Load children folders
            file.listFiles(FileFilter {
                it.isDirectory
            })?.forEach {
                PRELOADED_MAP_MUTEX.withLock {
                    if (PRELOADED_MAP[it.path] == null) {
                        val subfiles = it.list() ?: arrayOf()
                        for (filename in subfiles) {
                            Processor.addToProcess(it.path, ProcessUnit(it.path + DIVIDER + filename, unit.second))
                        }

                        PRELOADED_MAP[it.path] = PreloadedFolder(subfiles.size)
                    }
                }
            }

            //Load parent folder
            PRELOADED_MAP_MUTEX.withLock {
                val parentPath = file.parent
                if (parentPath != null && PRELOADED_MAP[parentPath] == null) {
                    val parentFileList: Array<KFile>? = file.parentFile?.listFiles()
                    if (parentFileList != null) {
                        parentFileList.forEach {
                            Processor.addToProcess(parentPath, ProcessUnit(it.path, unit.second))
                        }
                        PRELOADED_MAP[parentPath] = PreloadedFolder(parentFileList.size)
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
            val file = KFile(unit.first)
            val fileList = file.list() ?: arrayOf()

            PRELOAD_LIST_MUTEX.withLock {
                for (path in fileList) {
                    Processor.addToProcess(file.path, ProcessUnit(file.absolutePath + DIVIDER + path, unit.second))
                }
            }

            PRELOADED_MAP_MUTEX.withLock {
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
        val f: () -> ProcessedUnit = { load(path, unit) }
        PRELOAD_LIST.add(f)
    }

    /**
     * Gets the loaded files from the folder denoted by [path].
     *
     * @see work to understand.
     */
    suspend fun getLoaded(path: String): Pair<Boolean, List<DataContainer>>? {
        PRELOADED_MAP_MUTEX.withLock {
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
    suspend fun getAllData(): List<DataContainer>? {
        PRELOADED_MAP_MUTEX.withLock {
            val completeList = mutableListOf<DataContainer>()
            PRELOADED_MAP.map { completeList.addAll(it.value) }
            return completeList
        }
    }

    /**
     * NEVER CALL ON MAIN THREAD
     * Loads every element in PRELOAD_LIST
     */
    suspend fun work() {
        PRELOAD_LIST_MUTEX.withLock {
            PRELOAD_LIST.removeAll {
                val (path, data) = it.invoke()
                PRELOADED_MAP[path]!!.add(data)
            }
        }
    }

    /**
     * This loads every folder.
     */
    private fun load(path: String, unit: ProcessUnit) = ProcessedUnit(path, unit.second.invoke(unit.first))
}