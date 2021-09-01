package ch.smoca.redux
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/*
Store for redux like architecture
 */
class Store<T : State>(initialState: T) {

    private var state: T = initialState
    private val mainThreadActionListeners: MutableList<ActionListener> = mutableListOf()
    private val sagas: MutableList<Saga<T>> = mutableListOf()
    private val reducers: MutableList<Reducer<T>> = mutableListOf()
    private val singleThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val stateHolder = MutableStateFlow<T>(state)
    // Will return a live data to observe state change
    val stateObservable: StateFlow<T>
        get() = stateHolder

    // Will return a live data to observe the actions in the system.
    fun addMainThreadActionListener(listener: ActionListener) {
        mainThreadActionListeners.add(listener)
    }

    fun removeMainThreadActionListener(listener: ActionListener) {
        mainThreadActionListeners.remove(listener)
    }

    fun dispatch(action: Action) {

        CoroutineScope(singleThread).launch {

            val oldState = state
            state = reducers.fold(state) { preState, reducer -> reducer.reduce(action, preState) }
            for (saga in sagas) {
                saga.onAction(action, oldState, state)
            }
            if (state != oldState) {
                // we will not post the state if it did not change.
                // however, it is still possible that the UI receives the same state twice.
                // if something changes value fast to something and back, the UI thread may
                // receive the first state and the last state.
                stateHolder.value = state
            }

            alertListenerOnMainThread(action)
        }
    }

    // UI Action Listener will always be notified on the main thread. For every action
    private fun alertListenerOnMainThread(action: Action) {
        CoroutineScope(Dispatchers.Main).launch {
            for (listener in mainThreadActionListeners) listener.onAction(action)
        }
    }

    operator fun plusAssign(saga: Saga<T>) {
        sagas.add(saga)
    }

    operator fun plusAssign(reducer: Reducer<T>) {
        reducers.add(reducer)
    }
}

abstract class Saga<T : State>(val dispatch: (action: Action) -> Unit) {
    abstract fun onAction(action: Action, oldState: T, newState: T)
}

interface Reducer<T : State> {
    fun reduce(action: Action, state: T): T
}

interface ActionListener {
    fun onAction(action: Action)
}

interface State

interface Action
