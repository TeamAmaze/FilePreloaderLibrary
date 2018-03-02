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
* @see Processor.work
*/
internal typealias ProcessUnit<D> = Pair<String, FetcherFunction<D>>

/**
 * Contains the metadata (`[ProcessedUnit].second`) for a file inside the path `[ProcessedUnit].first`
 */
internal typealias ProcessedUnit<D> = Pair<String, D>

/**
 * The maximum allowed elements in [PRELOADED_MAP]
 */
private const val PRELOADED_MAP_MAXIMUM = 4*10000

/**
 * Singleton charged with writing to [preloadList] and starting the preload
 * and, afterwards reading from [preloadedList] and returning the output.
 */
internal class Processor<D: DataContainer>(private val clazz: Class<D>) {

    init {
        if(PreloadedManager.get(clazz) == null) {
            PreloadedManager.add(clazz)
        }
    }

    /**
     * Thread safe.
     * All the callable executions to load all the folders.
     *
     * 'Load a folder' means that the function `[unit].second` will be called
     * on each file (represented by its path) inside the folder.
     */
    private val preloadList: MutableList<() -> ProcessedUnit<D>> =
            Collections.synchronizedList(mutableListOf<() -> ProcessedUnit<D>>())
    private val preloadListMutex = Mutex()

    /**
     * Asynchly load every folder inside the path `[unit].first`.
     * It will even load the parent (aka '..'), as this method implies that the user can go up
     * (in the filesystem tree).
     *
     * 'Load a folder' means that the function `[unit].second` will be called
     * on each file (represented by its path) inside the folder.
     */
    internal fun workFrom(unit: ProcessUnit<D>) {
        launch {
            var somethingAddedToPreload = false
            val file = KFile(unit.first)

            //Load current folder
            getPreloadMapMutex().withLock {
                if (getPreloadMap()[file.path] == null) {
                    val subfiles: Array<String> = file.list() ?: arrayOf()
                    for (filename in subfiles) {
                        addToProcess(file.path, ProcessUnit(file.path + DIVIDER + filename, unit.second))
                    }

                    getPreloadMap()[file.path] = PreloadedFolder(subfiles.size)
                    if (getPreloadMap().size > PRELOADED_MAP_MAXIMUM) cleanOldEntries()
                    getDeleteQueue().add(file.path)

                    somethingAddedToPreload = somethingAddedToPreload || subfiles.isNotEmpty()
                }
            }

            //Load children folders
            file.listFiles(FileFilter {
                it.isDirectory
            })?.forEach {
                getPreloadMapMutex().withLock {
                    if (getPreloadMap()[it.path] == null) {
                        val subfiles = it.list() ?: arrayOf()
                        for (filename in subfiles) {
                            addToProcess(it.path, ProcessUnit(it.path + DIVIDER + filename, unit.second))
                        }

                        getPreloadMap()[it.path] = PreloadedFolder(subfiles.size)
                        if (getPreloadMap().size > PRELOADED_MAP_MAXIMUM) cleanOldEntries()
                        getDeleteQueue().add(it.path)

                        somethingAddedToPreload = somethingAddedToPreload || subfiles.isNotEmpty()
                    }
                }
            }

            //Load parent folder
            getPreloadMapMutex().withLock {
                val parentPath = file.parent
                if (parentPath != null && getPreloadMap()[parentPath] == null) {
                    val parentFileList: Array<KFile>? = file.parentFile?.listFiles()
                    if (parentFileList != null) {
                        parentFileList.forEach {
                            addToProcess(parentPath, ProcessUnit(it.path, unit.second))
                        }

                        getPreloadMap()[parentPath] = PreloadedFolder(parentFileList.size)
                        if (getPreloadMap().size > PRELOADED_MAP_MAXIMUM) cleanOldEntries()
                        getDeleteQueue().add(parentPath)

                        somethingAddedToPreload = somethingAddedToPreload || parentFileList.isNotEmpty()
                    }
                }
            }

            if(somethingAddedToPreload) {
                work()
            }
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

            getPreloadMapMutex().withLock {
                getPreloadMap()[file.path] = PreloadedFolder(fileList.size)
                getDeleteQueue().add(file.path)
            }

            if(fileList.isNotEmpty()) {
                work()
            }
        }
    }

    /**
     * Clear everything, all data loaded will be discarded.
     */
    internal fun clear() {
        preloadList.clear()
        getPreloadMap().clear()
        getDeleteQueue().clear()
    }

    /**
     * Add file (represented by [unit]) to the [preloadList] to be preloaded by [work].
     */
    private fun addToProcess(path: String, unit: ProcessUnit<D>) {
        val f: () -> ProcessedUnit<D> = { load(path, unit) }
        preloadList.add(f)
    }

    /**
     * Gets the loaded files from the folder denoted by [path].
     *
     * @see work to understand.
     */
    internal suspend fun getLoaded(path: String): Pair<Boolean, List<D>>? {
        getPreloadMapMutex().withLock {
            val completeSet = getPreloadMap()[path]
            if (completeSet == null) return null
            else return completeSet.isComplete() to completeSet.toList()
        }
    }

    /**
     * *ONLY USE FOR DEBUGGING*
     * This function gets every file metadata loaded.
     */
    internal suspend fun getAllData(): List<DataContainer>? {
        getPreloadMapMutex().withLock {
            val completeList = mutableListOf<DataContainer>()
            getPreloadMap().map { completeList.addAll(it.value) }
            return completeList
        }
    }

    /**
     * Calls each function in [preloadList] (removing it).
     * Then adds the result [(path, data)] to `[getPreloadMap].get(path)`.
     */
    private suspend fun work() {
        preloadListMutex.withLock {
            preloadList.removeAll {
                val (path, data) = it.invoke()

                val list = getPreloadMap()[path]
                        ?: throw IllegalStateException("A list has been deleted before elements were added. We are VERY out of memory!")
                list.add(data)
            }
        }
    }

    /**
     * Cleans entries in [getPreloadMap] to free memory.
     */
    private fun cleanOldEntries() {
        for (i in 0..PRELOADED_MAP_MAXIMUM / 4) {
            if (!getDeleteQueue().isEmpty()) {
                getPreloadMap().remove(getDeleteQueue().remove())
            } else break
        }
    }

    /**
     * This loads every folder.
     */
    private fun load(path: String, unit: ProcessUnit<D>): ProcessedUnit<D> {
        return ProcessedUnit(path, unit.second.process(unit.first))
    }

    /**
     * Gets the map for [D].
     *
     * @see PreloadedManager
     */
    private fun getPreloadMap(): PreloadedFoldersMap<D> {
        val data = PreloadedManager.get(clazz) ?: throw NullPointerException("No map for $clazz!")
        return data.preloadedFoldersMap
    }

    /**
     * Gets the deleteQueue for [D].
     *
     * Thread safe.
     * If entries must be deleted from [getPreloadMap] in the order given by [deletionQueue].remove().
     *
     * @see PreloadedManager
     */
    private fun getDeleteQueue(): UniqueQueue {
        val data = PreloadedManager.get(clazz) ?: throw NullPointerException("No map for $clazz!")
        return data.deleteQueue
    }

    /**
     * Gets the mutex for [getPreloadMap] for [D].
     *
     * @see PreloadedManager
     */
    private fun getPreloadMapMutex() = PreloadedManager.getMutex(clazz) ?: throw NullPointerException("No Mutex for $clazz!")
}