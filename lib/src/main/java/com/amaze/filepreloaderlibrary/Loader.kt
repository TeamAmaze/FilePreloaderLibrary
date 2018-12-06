package com.amaze.filepreloaderlibrary

import com.amaze.filepreloaderlibrary.PreloadedManager.getDeleteQueue
import com.amaze.filepreloaderlibrary.PreloadedManager.getPreloadMap
import com.amaze.filepreloaderlibrary.PreloadedManager.getPreloadMapMutex
import com.amaze.filepreloaderlibrary.datastructures.DataContainer
import com.amaze.filepreloaderlibrary.datastructures.PreloadableUnit
import com.amaze.filepreloaderlibrary.datastructures.PreloadedFolder
import com.amaze.filepreloaderlibrary.utils.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

/**
 * The maximum allowed elements in [PRELOADED_MAP]
 */
private const val PRELOADED_MAP_MAXIMUM = 4*10000

internal class Loader<D: DataContainer>(private val clazz: Class<D>) {

    val processor = Processor(clazz)

    /**
     * Asynchly load every folder inside the path `[unit].first`.
     * It will even load the parent (aka '..'), as this method implies that the user can go up
     * (in the filesystem tree).
     *
     * 'Load a folder' means that the function `[unit].second` will be called
     * on each file (represented by its path) inside the folder.
     */
    internal fun loadFrom(unit: ProcessUnit<D>) {
        GlobalScope.launch {
            var somethingAddedToPreload = false
            val file = KFile(unit.path)

            //Load current folder
            getPreloadMapMutex(clazz).withLock {
                if (getPreloadMap(clazz)[file.path] == null) {
                    val subfiles: Array<String> = file.list() ?: arrayOf()
                    for (filename in subfiles) {
                        addToProcess(file.path, ProcessUnit(file.path + DIVIDER + filename, unit.fetcherFunction), PRIORITY_NOW)
                    }

                    getPreloadMap(clazz)[file.path] = PreloadedFolder(subfiles.size)
                    if (getPreloadMap(clazz).size > PRELOADED_MAP_MAXIMUM) cleanOldEntries()
                    getDeleteQueue(clazz).add(file.path)

                    somethingAddedToPreload = somethingAddedToPreload || subfiles.isNotEmpty()
                }
            }

            //Load children folders
            file.listDirectoriesToList()?.forEach {
                getPreloadMapMutex(clazz).withLock {
                    val currentPath = unit.path + DIVIDER + it

                    if (getPreloadMap(clazz)[currentPath] == null) {
                        val subfiles: Array<String> = KFile(currentPath).list() ?: arrayOf()
                        for (filename in subfiles) {
                            addToProcess(currentPath, ProcessUnit(currentPath + DIVIDER + filename, unit.fetcherFunction), PRIORITY_FUTURE)
                        }

                        getPreloadMap(clazz)[currentPath] = PreloadedFolder(subfiles.size)
                        if (getPreloadMap(clazz).size > PRELOADED_MAP_MAXIMUM) cleanOldEntries()
                        getDeleteQueue(clazz).add(currentPath)

                        somethingAddedToPreload = somethingAddedToPreload || subfiles.isNotEmpty()
                    }
                }
            }

            //Load parent folder
            getPreloadMapMutex(clazz).withLock {
                val parentPath = file.parent
                if (parentPath != null && getPreloadMap(clazz)[parentPath] == null) {
                    val subfiles: Array<String> = KFile(parentPath).list() ?: arrayOf()
                    subfiles.forEach {
                        addToProcess(parentPath, ProcessUnit(parentPath + DIVIDER + it, unit.fetcherFunction), PRIORITY_FUTURE)
                    }

                    getPreloadMap(clazz)[parentPath] = PreloadedFolder(subfiles.size)
                    if (getPreloadMap(clazz).size > PRELOADED_MAP_MAXIMUM) cleanOldEntries()
                    getDeleteQueue(clazz).add(parentPath)

                    somethingAddedToPreload = somethingAddedToPreload || subfiles.isNotEmpty()
                }
            }

            if(somethingAddedToPreload) {
                processor.work()
            }
        }
    }

    /**
     * Asynchly loads the folder in `[unit].first`.
     *
     * 'Load a folder' means that the function `[unit].second` will be called
     * on each file (represented by its path) inside the folder.
     */
    internal fun loadFolder(unit: ProcessUnit<D>) {
        GlobalScope.launch {
            val file = KFile(unit.path)
            val fileList = file.list() ?: arrayOf()

            for (path in fileList) {
                addToProcess(file.path, ProcessUnit(file.absolutePath + DIVIDER + path, unit.fetcherFunction), PRIORITY_NOW)
            }

            getPreloadMapMutex(clazz).withLock {
                getPreloadMap(clazz)[file.path] = PreloadedFolder(fileList.size)
                getDeleteQueue(clazz).add(file.path)
            }

            if (fileList.isNotEmpty()) {
                processor.work()
            }
        }
    }

    /**
     * Cleans entries in [getPreloadMap] to free memory.
     */
    private fun cleanOldEntries() {
        for (i in 0..PRELOADED_MAP_MAXIMUM / 4) {
            if (!PreloadedManager.getDeleteQueue(clazz).isEmpty()) {
                getPreloadMap(clazz).remove(PreloadedManager.getDeleteQueue(clazz).remove())
            } else break
        }
    }

    /**
     * Clear everything, all data loaded will be discarded.
     */
    internal suspend fun clear() {
        processor.clear()
        getPreloadMap(clazz).clear()
        getDeleteQueue(clazz).clear()
    }

    /**
     * Add file (represented by [unit]) to the [preloadPriorityQueue] to be preloaded by [loadFolder].
     */
    private suspend fun addToProcess(path: String, unit: ProcessUnit<D>, priority: Int) {
        val start = if(priority == PRIORITY_NOW) CoroutineStart.DEFAULT else CoroutineStart.LAZY

        val f = GlobalScope.async(start = start) { ProcessedUnit(path, unit.fetcherFunction(unit.path)) }
        processor.add(PreloadableUnit(f, priority, unit.path.hashCode()))
    }

    /**
     * Gets the loaded files from the folder denoted by [path].
     *
     * @see loadFolder to understand.
     */
    internal suspend fun getLoaded(path: String): Pair<Boolean, List<D>>? {
        getPreloadMapMutex(clazz).withLock {
            val completeSet = getPreloadMap(clazz)[path]
            if (completeSet == null) return null
            else return completeSet.isComplete() to completeSet.toList()
        }
    }

    /**
     * When a [PreloadedFolder] is fully loaded the [listener] will be called
     * If the path isn't going to be loaded the [listener] is invoked with argument null
     */
    internal suspend fun setCompletionListener(path: String, listener: (List<D>?)->Unit) {
        getPreloadMapMutex(clazz).withLock {
            val completeSet = getPreloadMap(clazz)[path]
            if (completeSet == null) listener.invoke(null)
            else completeSet.listener = {
                listener.invoke(it.toList())

                DebugLog.log("FilePreloader.Complete", "$path is complete!")
            }
        }
    }


}