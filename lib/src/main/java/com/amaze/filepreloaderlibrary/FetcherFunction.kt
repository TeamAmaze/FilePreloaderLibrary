package com.amaze.filepreloaderlibrary

class FetcherFunction<out D: DataContainer> (private val f: (String) -> D) {
    internal fun process(path: String) = f(path)
}