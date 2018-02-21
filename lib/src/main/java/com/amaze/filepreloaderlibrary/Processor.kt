package com.amaze.filepreloaderlibrary

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
internal typealias ProcessUnit<D> = Pair<String, FetcherFunction<D>>

/**
 * Contains the data (`[ProcessedUnit].second`) for a file inside the path `[ProcessedUnit].first`
 */
internal typealias ProcessedUnit<D> = Pair<String, D>

/**
 * The maximum allowed elements in [PRELOADED_MAP]
 */
private const val PRELOADED_MAP_MAXIMUM = 4*10000

/**
 * Singleton charged with writing to [mutableList] and starting the preload
 * and, afterwards reading from [preloadedList] and returning the output.
 */
internal class Processor<D: DataContainer> {
    /**
     * Thread safe.
     * All the callable executions to load all the folders.
     *
     * 'Load a folder' means that the function `[unit].second` will be called
     * on each file (represented by its path) inside the folder.
     */
    private val mutableList: MutableList<() -> ProcessedUnit<D>> =
            Collections.synchronizedList(mutableListOf<() -> ProcessedUnit<D>>())
    private val preloadListMutex = Mutex()

    /**
     * Thread safe.
     * Maps each folder to a list of [DataContainer] with each file's data.
     * The first folder is passed in [Processor.workFrom] or [Processor.work] to get the result of the
     * load folder operation.
     *
     * 'Load a folder' means that the function `[unit].second` will be called
     * on each file (represented by its path) inside the folder.
     */
    private val preloadedList: MutableMap<String, PreloadedFolder<D>> =
            Collections.synchronizedMap(hashMapOf<String, PreloadedFolder<D>>())
    private val preloadedListMutex = Mutex()

    /**
     * Thread safe.
     * If entries must be deleted from [PRELOADED_MAP] in the order given by [deletionQueue].remove().
     */
    private val deletionQueue: UniqueQueue = UniqueQueue()

    /**
     * Asynchly load every folder inside the path `[unit].first` except itself (aka '.').
     * It will even load the parent (aka '..'), as this method implies that the user can go up
     * (in the filesystem tree).
     *
     * 'Load a folder' means that the function `[unit].second` will be called
     * on each file (represented by its path) inside the folder.
     */
    internal fun workFrom(unit: ProcessUnit<D>) {
        launch {
            val file = KFile(unit.first)

            //Load current folder
            preloadedListMutex.withLock {
                if (preloadedList[file.path] == null) {
                    val subfiles: Array<String> = file.list() ?: arrayOf()
                    for (filename in subfiles) {
                        addToProcess(file.path, ProcessUnit(file.path + DIVIDER + filename, unit.second))
                    }

                    preloadedList[file.path] = PreloadedFolder(subfiles.size)
                    if (preloadedList.size > PRELOADED_MAP_MAXIMUM) cleanOldEntries()
                    deletionQueue.add(file.path)
                }
            }

            //Load children folders
            file.listFiles(FileFilter {
                it.isDirectory
            })?.forEach {
                preloadedListMutex.withLock {
                    if (preloadedList[it.path] == null) {
                        val subfiles = it.list() ?: arrayOf()
                        for (filename in subfiles) {
                            addToProcess(it.path, ProcessUnit(it.path + DIVIDER + filename, unit.second))
                        }

                        preloadedList[it.path] = PreloadedFolder(subfiles.size)
                        if (preloadedList.size > PRELOADED_MAP_MAXIMUM) cleanOldEntries()
                        deletionQueue.add(it.path)
                    }
                }
            }

            //Load parent folder
            preloadedListMutex.withLock {
                val parentPath = file.parent
                if (parentPath != null && preloadedList[parentPath] == null) {
                    val parentFileList: Array<KFile>? = file.parentFile?.listFiles()
                    if (parentFileList != null) {
                        parentFileList.forEach {
                            addToProcess(parentPath, ProcessUnit(it.path, unit.second))
                        }

                        preloadedList[parentPath] = PreloadedFolder(parentFileList.size)
                        if (preloadedList.size > PRELOADED_MAP_MAXIMUM) cleanOldEntries()
                        deletionQueue.add(parentPath)
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
    internal fun work(unit: ProcessUnit<D>) {
        launch {
            val file = KFile(unit.first)
            val fileList = file.list() ?: arrayOf()

            preloadListMutex.withLock {
                for (path in fileList) {
                    addToProcess(file.path, ProcessUnit(file.absolutePath + DIVIDER + path, unit.second))
                }
            }

            preloadedListMutex.withLock {
                preloadedList[file.path] = PreloadedFolder(fileList.size)
                deletionQueue.add(file.path)
            }
            work()
        }
    }

    /**
     * Clear everything, all data loaded will be discarded.
     */
    internal fun clear() {
        mutableList.clear()
        preloadedList.clear()
        deletionQueue.clear()
    }

    /**
     * Add file (represented by [unit]) to the [mutableList] to be preloaded by [Threader].
     */
    private fun addToProcess(path: String, unit: ProcessUnit<D>) {
        val f: () -> ProcessedUnit<D> = { load(path, unit) }
        mutableList.add(f)
    }

    /**
     * Gets the loaded files from the folder denoted by [path].
     *
     * @see work to understand.
     */
    internal suspend fun getLoaded(path: String): Pair<Boolean, List<D>>? {
        preloadedListMutex.withLock {
            val completeSet = preloadedList[path]
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
    internal suspend fun getAllData(): List<DataContainer>? {
        preloadedListMutex.withLock {
            val completeList = mutableListOf<DataContainer>()
            preloadedList.map { completeList.addAll(it.value) }
            return completeList
        }
    }

    /**
     * NEVER CALL ON MAIN THREAD
     * Loads every element in mutableList
     */
    private suspend fun work() {
        preloadListMutex.withLock {
            mutableList.removeAll {
                val (path, data) = it.invoke()

                val list = preloadedList[path]
                        ?: throw IllegalStateException("A list has been deleted before elements were added. We are VERY out of memory!")
                list.add(data)
            }
        }
    }

    private fun cleanOldEntries() {
        for (i in 0..PRELOADED_MAP_MAXIMUM / 4) {
            if (!deletionQueue.isEmpty()) {
                preloadedList.remove(deletionQueue.remove())
            } else break
        }
    }

    /**
     * This loads every folder.
     */
    private fun load(path: String, unit: ProcessUnit<D>): ProcessedUnit<D> {
        return ProcessedUnit(path, unit.second.process(unit.first))
    }
}