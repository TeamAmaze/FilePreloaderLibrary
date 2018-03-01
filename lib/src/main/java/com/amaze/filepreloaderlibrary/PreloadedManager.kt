package com.amaze.filepreloaderlibrary

import com.amaze.filepreloaderlibrary.PreloadedManager.ProcessorData
import com.amaze.filepreloaderlibrary.PreloadedManager.preloadedObjectMutexMap
import com.amaze.filepreloaderlibrary.PreloadedManager.preloadedObjectsMap
import kotlinx.coroutines.experimental.sync.Mutex
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

    data class ProcessorData<D: DataContainer>(val deleteQueue: UniqueQueue,
                                               val preloadedFoldersMap: PreloadedFoldersMap<D>)

}