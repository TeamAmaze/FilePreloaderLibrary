package com.amaze.filepreloaderlibrary

import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.sync.withLock
import java.util.concurrent.PriorityBlockingQueue

/**
* Basically means call `[ProcessUnit].fetcherFunction` on each of `[ProcessUnit].path`'s files.
* This is done asynchly.
*
* @see Processor.work
*/
internal data class ProcessUnit<out D: DataContainer>(val path: String, val fetcherFunction: FetcherFunction<D>)

/**
 * Contains the [ProcessedUnit].metadataObject for a file inside [ProcessedUnit].path
 */
internal data class ProcessedUnit<out D: DataContainer>(val path: String, val metadataObject: D)

internal data class PreloadableUnit<D: DataContainer>(val function: () -> ProcessedUnit<D>, val priority: Int): Comparable<PreloadableUnit<D>> {
    override fun compareTo(other: PreloadableUnit<D>) = priority.compareTo(other.priority)
}

/**
 * The maximum allowed elements in [PRELOADED_MAP]
 */
private const val PRELOADED_MAP_MAXIMUM = 4*10000

/**
 * Singleton charged with writing to [preloadPriorityQueue] and starting the preload
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
    private val preloadPriorityQueue: PriorityBlockingQueue<PreloadableUnit<D>> = PriorityBlockingQueue()

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
            val file = KFile(unit.path)

            //Load current folder
            getPreloadMapMutex().withLock {
                if (getPreloadMap()[file.path] == null) {
                    val subfiles: Array<String> = file.list() ?: arrayOf()
                    for (filename in subfiles) {
                        addToProcess(file.path, ProcessUnit(file.path + DIVIDER + filename, unit.fetcherFunction), PRIORITY_NOW)
                    }

                    getPreloadMap()[file.path] = PreloadedFolder(subfiles.size)
                    if (getPreloadMap().size > PRELOADED_MAP_MAXIMUM) cleanOldEntries()
                    getDeleteQueue().add(file.path)

                    somethingAddedToPreload = somethingAddedToPreload || subfiles.isNotEmpty()
                }
            }

            //Load children folders
            file.listDirectoriesToList()?.forEach {
                getPreloadMapMutex().withLock {
                    val currentPath = unit.path + DIVIDER + it

                    if (getPreloadMap()[currentPath] == null) {
                        val subfiles: Array<String> = KFile(currentPath).list() ?: arrayOf()
                        for (filename in subfiles) {
                            addToProcess(currentPath, ProcessUnit(currentPath + DIVIDER + filename, unit.fetcherFunction), PRIORITY_FUTURE)
                        }

                        getPreloadMap()[currentPath] = PreloadedFolder(subfiles.size)
                        if (getPreloadMap().size > PRELOADED_MAP_MAXIMUM) cleanOldEntries()
                        getDeleteQueue().add(currentPath)

                        somethingAddedToPreload = somethingAddedToPreload || subfiles.isNotEmpty()
                    }
                }
            }

            //Load parent folder
            getPreloadMapMutex().withLock {
                val parentPath = file.parent
                if (parentPath != null && getPreloadMap()[parentPath] == null) {
                    val subfiles: Array<String> = KFile(parentPath).list() ?: arrayOf()
                    subfiles.forEach {
                        addToProcess(parentPath, ProcessUnit(parentPath + DIVIDER + it, unit.fetcherFunction), PRIORITY_POSSIBLY)
                    }

                    getPreloadMap()[parentPath] = PreloadedFolder(subfiles.size)
                    if (getPreloadMap().size > PRELOADED_MAP_MAXIMUM) cleanOldEntries()
                    getDeleteQueue().add(parentPath)

                    somethingAddedToPreload = somethingAddedToPreload || subfiles.isNotEmpty()
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
            val file = KFile(unit.path)
            val fileList = file.list() ?: arrayOf()

            synchronized(preloadPriorityQueue) {
                for (path in fileList) {
                    addToProcess(file.path, ProcessUnit(file.absolutePath + DIVIDER + path, unit.fetcherFunction), PRIORITY_NOW)
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
        preloadPriorityQueue.clear()
        getPreloadMap().clear()
        getDeleteQueue().clear()
    }

    /**
     * Add file (represented by [unit]) to the [preloadPriorityQueue] to be preloaded by [work].
     */
    private fun addToProcess(path: String, unit: ProcessUnit<D>, priority: Int) {
        val f: () -> ProcessedUnit<D> = { load(path, unit) }
        preloadPriorityQueue.add(PreloadableUnit(f, priority))
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
     * When a [PreloadedFolder] is fully loaded the [listener] will be called
     * If the path isn't going to be loaded the [listener] is invoked with argument null
     */
    internal suspend fun setCompletionListener(path: String, listener: (List<D>?)->Unit) {
        getPreloadMapMutex().withLock {
            val completeSet = getPreloadMap()[path]
            if (completeSet == null) listener.invoke(null)
            else completeSet.listener = {
                listener.invoke(it.toList())

                DebugLog.log("FilePreloader.Complete", "$path is complete!")
            }
        }
    }

    /**
     * Calls each function in [preloadPriorityQueue] (removing it).
     * Then adds the result [(path, data)] to `[getPreloadMap].get(path)`.
     */
    private fun work() {
        launch {
            while (preloadPriorityQueue.isNotEmpty()) {
                val elem = preloadPriorityQueue.poll()
                val (path, data) = elem.function()

                DebugLog.log("FilePreloader.Processor", "[P${elem.priority}] Loading from $path: $data")

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
        return ProcessedUnit(path, unit.fetcherFunction.process(unit.path))
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