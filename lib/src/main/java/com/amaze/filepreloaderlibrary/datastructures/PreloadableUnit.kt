package com.amaze.filepreloaderlibrary.datastructures

import com.amaze.filepreloaderlibrary.ProcessedUnit
import kotlinx.coroutines.Deferred

internal data class PreloadableUnit<D: DataContainer>(val future: Deferred<ProcessedUnit<D>>, val priority: Int): Comparable<PreloadableUnit<D>> {
    override fun compareTo(other: PreloadableUnit<D>) = priority.compareTo(other.priority)
}