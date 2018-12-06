package com.amaze.filepreloaderlibrary

import com.amaze.filepreloaderlibrary.PreloadedManager.ProcessorData
import com.amaze.filepreloaderlibrary.PreloadedManager.preloadedObjectMutexMap
import com.amaze.filepreloaderlibrary.PreloadedManager.preloadedObjectsMap
import com.amaze.filepreloaderlibrary.datastructures.DataContainer
import com.amaze.filepreloaderlibrary.datastructures.UniqueQueue
import com.amaze.filepreloaderlibrary.utils.PreloadedFoldersMap
import kotlinx.coroutines.sync.Mutex
import java.util.*

/**
 * This singleton maintains a reference to [preloadedObjectsMap] that maps each type to a [ProcessorData].
 * Because the user can provide any object as metadata container (as long as it extends [DataContainer])
 * this has to deal with deciphering which preloaded metadata the user wants.
 * There is also a [preloadedObjectMutexMap] that should be used to ensure
 * that concurrent operations are safe.
 */
object PreloadedManager {
    private val preloadedObjectsMap: MutableMap<Class<out DataContainer>, ProcessorData<out DataContainer>> =
            hashMapOf()
    private val preloadedObjectMutexMap: MutableMap<Class<out DataContainer>, Mutex> = hashMapOf()


    internal fun add(clazz: Class<out DataContainer>) {
        preloadedObjectsMap[clazz] = ProcessorData(UniqueQueue(), Collections.synchronizedMap(hashMapOf()))
        preloadedObjectMutexMap[clazz] = Mutex()
    }

    internal fun <D: DataContainer>get(clazz: Class<D>): ProcessorData<D>? {
        return preloadedObjectsMap[clazz] as ProcessorData<D>?
    }

    internal fun getMutex(clazz: Class<out DataContainer>): Mutex? {
        return preloadedObjectMutexMap[clazz]
    }

    internal fun getAllLoaded(): List<DataContainer> {
        val dataList = mutableListOf<DataContainer>()

        preloadedObjectsMap.forEach {
            it.value.preloadedFoldersMap.forEach {
                it.value.forEach {
                    dataList.add(it)
                }
            }
        }

        return dataList;
    }

    data class ProcessorData<D: DataContainer>(val deleteQueue: UniqueQueue,
                                               val preloadedFoldersMap: PreloadedFoldersMap<D>)


    /**
     * Gets the map for [D].
     *
     * @see PreloadedManager
     */
    internal fun <D: DataContainer> getPreloadMap(clazz: Class<D>): PreloadedFoldersMap<D> {
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
    internal fun getDeleteQueue(clazz: Class<out DataContainer>): UniqueQueue {
        val data = PreloadedManager.get(clazz) ?: throw NullPointerException("No map for $clazz!")
        return data.deleteQueue
    }

    /**
     * Gets the mutex for [getPreloadMap] for [D].
     *
     * @see PreloadedManager
     */
    internal fun getPreloadMapMutex(clazz: Class<out DataContainer>) =
            PreloadedManager.getMutex(clazz) ?: throw NullPointerException("No Mutex for $clazz!")
}