package com.amaze.filepreloaderlibrary

/**
 * This class is a more legible version of `f: (String) -> D` that can be called with [process]
 * instead of invoke.
 */
class FetcherFunction<out D: DataContainer> (private val f: (String) -> D) {
    internal fun process(path: String) = f(path)
}