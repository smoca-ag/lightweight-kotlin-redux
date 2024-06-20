package ch.smoca.redux

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Store for redux like architecture
 * If used with jetpack compose, make sure to mark the store as @Stable
 * @param T the type of your initial state
 * @param initialState the initial state
 */
abstract class Store<T : State>(initialState: T) {
    private var state: T = initialState
    private val mainThreadStateListener: MutableList<StateListener> = mutableListOf()
    private val sagas: MutableList<Saga<T>> = mutableListOf()
    private val reducers: MutableList<Reducer<T>> = mutableListOf()
    private val singleThread = Dispatchers.IO.limitedParallelism(1)
    private val stateHolder = MutableStateFlow(state)

    fun addReducer(reducer: Reducer<T>) {
        reducers.add(reducer)
    }

    fun addReducers(reducers: List<Reducer<T>>) {
        reducers.forEach { addReducer(it) }
    }

    fun addSaga(saga: Saga<T>) {
        sagas.add(saga)
    }

    fun addSagas(sagas: List<Saga<T>>) {
        sagas.forEach { addSaga(it) }
    }

    /**
     * @return StateFlow<T> to observe state change where T is the type of the state
     */
    val stateObservable: StateFlow<T>
        get() = stateHolder

    /**
     * Registers a state listener
     */
    fun addStateListener(listener: StateListener) {
        mainThreadStateListener.add(listener)
    }

    /**
     * Removes a state listener
     */
    fun removeStateListener(listener: StateListener) {
        mainThreadStateListener.remove(listener)
    }

    /**
     * Dispatches an action to the main thread, no matter from which thread it is called.
     * All action listeners are alerted.
     * @param action an action to be dispatched
     */
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
                alertListenerOnMainThread(state)
            }
        }
    }

    // UI state listener will always be notified on the main thread
    private fun alertListenerOnMainThread(state: T) {
        CoroutineScope(Dispatchers.Main).launch {
            for (listener in mainThreadStateListener) listener.onStateChanged(state)
        }
    }
}
