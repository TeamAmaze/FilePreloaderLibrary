package com.amaze.filepreloaderlibrary

import com.amaze.filepreloaderlibrary.PreloadedManager.preloadedObjectMutexMap
import com.amaze.filepreloaderlibrary.PreloadedManager.preloadedObjectsMap
import kotlinx.coroutines.experimental.sync.Mutex
import java.util.*

/**
 * This singleton maintains a reference to [preloadedObjectsMap] that maps each type to a map containing metadata.
 * Because the user can provide any object as metadata container (as long as it extends [DataContainer])
 * this has to deal with deciphering which preloaded metadata the user wants.
 * There is also a [preloadedObjectMutexMap] that should be used to ensure
 * that concurrent operations are safe.
 */
object PreloadedManager {
    private val preloadedObjectsMap: MutableMap<Class<out DataContainer>, MutableMap<String, PreloadedFolder<out DataContainer>>> =
            hashMapOf()
    private val preloadedObjectMutexMap: MutableMap<Class<out DataContainer>, Mutex> = hashMapOf()


    internal fun add(clazz: Class<out DataContainer>) {
        preloadedObjectsMap[clazz] = Collections.synchronizedMap(hashMapOf())
        preloadedObjectMutexMap[clazz] = Mutex()
    }

    internal fun get(clazz: Class<out DataContainer>): MutableMap<String, PreloadedFolder<out DataContainer>>? {
        return preloadedObjectsMap[clazz]
    }

    internal fun getMutex(clazz: Class<out DataContainer>): Mutex? {
        return preloadedObjectMutexMap[clazz]
    }

}