package ch.smoca.redux.stateobservers

import ch.smoca.redux.Action
import ch.smoca.redux.Middleware
import ch.smoca.redux.State
import ch.smoca.redux.Store
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class StateObserverMiddleware<T : State>(
    list: List<StateObserver>,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO
) : Middleware<T> {

    data class Context(val observer: StateObserver, val dispatcher: CoroutineDispatcher)
    @OptIn(ExperimentalCoroutinesApi::class)
    private val observers = list.map {
        Context(it, coroutineDispatcher.limitedParallelism(1))
    }

    override fun process(
        action: Action,
        store: Store<T>,
        next: (action: Action) -> Unit
    ) {
        val oldState = store.getState()
        next(action)
        val state = store.getState()

        if (oldState != state) {
            observers.forEach { context ->
                CoroutineScope(context.dispatcher).launch {
                    context.observer.onStateChanged(state)
                }
            }
        }
    }
}