package ch.smoca.redux.stateobservers

import ch.smoca.redux.Action
import ch.smoca.redux.Middleware
import ch.smoca.redux.State
import ch.smoca.redux.Store
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class StateObserverMiddleware<T : State>(
    list: List<StateObserver<T>>,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Middleware<T> {

    data class Context<T : State>(
        val observer: StateObserver<T>,
        val dispatcher: CoroutineDispatcher,
    )

    private val observers = list.map {
        Context(it, coroutineDispatcher.limitedParallelism(1))
    }

    override fun apply(store: Store<T>, next: (action: Action) -> Unit): (action: Action) -> Unit {
        observers.forEach { context ->
            context.observer.dispatch = store::dispatch
        }
        return super.apply(store, next)
    }

    override fun process(
        action: Action,
        store: Store<T>,
        next: (action: Action) -> Unit,
    ) {
        val oldState = store.getState()
        next(action)
        val state = store.getState()

        if (oldState != state) {
            observers.forEach { context ->
                val oldSubState = context.observer.selectSubState(oldState)
                val newSubState = context.observer.selectSubState(state)

                if (oldSubState != newSubState) {
                    CoroutineScope(context.dispatcher).launch {
                        context.observer.onStateChanged(state)
                    }
                }
            }
        }
    }
}