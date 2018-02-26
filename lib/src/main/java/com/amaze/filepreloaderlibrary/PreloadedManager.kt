package com.amaze.filepreloaderlibrary

import kotlinx.coroutines.experimental.sync.Mutex
import java.util.*


object PreloadedManager {
    private val preloadedList: MutableMap<Class<out DataContainer>, MutableMap<String, PreloadedFolder<out DataContainer>>> =
            hashMapOf()
    private val preloadedListMutex: MutableMap<Class<out DataContainer>, Mutex> = hashMapOf()


    internal fun add(clazz: Class<out DataContainer>) {
        preloadedList[clazz] = Collections.synchronizedMap(hashMapOf())
        preloadedListMutex[clazz] = Mutex()
    }

    internal fun get(clazz: Class<out DataContainer>): MutableMap<String, PreloadedFolder<out DataContainer>>? {
        return preloadedList[clazz]
    }

    internal fun getMutex(clazz: Class<out DataContainer>): Mutex? {
        return preloadedListMutex[clazz]
    }

}