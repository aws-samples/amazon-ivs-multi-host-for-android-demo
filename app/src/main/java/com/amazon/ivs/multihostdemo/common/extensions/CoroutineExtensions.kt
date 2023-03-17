package com.amazon.ivs.multihostdemo.common.extensions

import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import kotlinx.coroutines.*
import timber.log.Timber

val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

/**
 * Launches a coroutine bound to this [Fragment].
 * Use for collecting and displaying flows from a ViewModel.
 */
fun Fragment.launchUI(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    block: suspend CoroutineScope.() -> Unit
) = viewLifecycleOwner.lifecycleScope.launch(
    context = CoroutineExceptionHandler { _, e ->
        Timber.e(e, "Coroutine failed: ${e.localizedMessage}")
    }
) {
    repeatOnLifecycle(state = lifecycleState, block = block)
}

/**
 * Launches a coroutine bound to this [ViewModel].
 * Use for any coroutine work in a ViewModel.
 */
fun ViewModel.launch(block: suspend CoroutineScope.() -> Unit) = viewModelScope.launch(
    context = CoroutineExceptionHandler { _, e ->
        Timber.d(e, "Coroutine failed: ${e.localizedMessage}")
    },
    block = block,
)
