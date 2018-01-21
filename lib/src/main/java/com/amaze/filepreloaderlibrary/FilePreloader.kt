package com.amaze.filepreloaderlibrary

import java.io.File

/**
 * @author Emmanuel Messulam <emmanuelbendavid@gmail.com>
 *              on 10/1/2018, at 15:04.
 */

/**
 * Use this class to interact with the library.
 */
object FilePreloader {

    /**
     * Asynchly preload every subfolder in this [path] (exept '.'),
     * the [instantiator] is used to create the `[D]: DataContainer` objects.
     */
    fun <D: DataContainer>preloadFrom(path: String, instantiator: (String) -> D) {
        Processor.workFrom(ProcessUnit(path, instantiator))
    }

    /**
     * Asynchly preload folder (denoted by its [path]),
     * the [instantiator] is used to create the `[D]: DataContainer` objects
     */
    fun <D: DataContainer>preload(path: String, instatiator: (String) -> D) {
        Processor.work(ProcessUnit(path, instatiator))
    }

    /**
     * Get the loaded data, this will load the data in the current thread if it's not loaded.
     *
     * @see preload
     */
    fun <D: DataContainer>load(path: String, instatiator: (String) -> D): List<D> {
        val t:Pair<Boolean, List<DataContainer>>? = Processor.getLoaded(path)

        return if(t != null && t.first) t.second as List<D>
        else {
            var path = path
            if(!path.endsWith(DIVIDER)) path += DIVIDER
            File(path).list().map { instatiator.invoke(path + it) }
        }
    }

    /**
     * *This function is only to test what data is being preloaded.*
     * Get all the loaded data, this will load the data in the current thread if it's not loaded.
     */
    fun <D: DataContainer>getAllDataLoaded(): List<D>? {
        val preloaded = Processor.getAllData()
        if(preloaded != null && preloaded.isNotEmpty()) return preloaded as List<D>//todo fix
        else return null
    }
}

