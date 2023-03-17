package com.amazon.ivs.stagebroadcastmanager.common

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

fun launchMain(block: suspend CoroutineScope.() -> Unit) = mainScope.launch(
    context = CoroutineExceptionHandler { _, e ->
        Timber.w(e, "Coroutine failed: ${e.localizedMessage}")
    },
    block = block
)

/**
 * Updates the [List] in the [StateFlow] calling the function block with the [List] as a receiver value.
 *
 * Used mostly for adding or removing items from the [List].
 */
fun <T> MutableStateFlow<List<T>>.updateList(block: MutableList<T>.() -> Unit) = update {
    it.toMutableList().apply(block = block)
}
