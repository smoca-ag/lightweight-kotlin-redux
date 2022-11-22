package ch.smoca.redux

import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Store for redux like architecture
 * @param T the type of your initial state
 * @param initialState the initial state
 */
class Store<T : State, AD: Any>(initialState: T) {
    private var state: T = initialState
    private val mainThreadActionListeners: MutableList<ActionListener<AD>> = mutableListOf()
    private val sagas: MutableList<Saga<T, AD, AD>> = mutableListOf()
    private val reducers: MutableList<Reducer<T, AD>> = mutableListOf()
    private val singleThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val stateHolder = MutableStateFlow<T>(state)

    /**
     * @return StateFlow<T> to observe state change where T is the type of the state
     */
    val stateObservable: StateFlow<T>
        get() = stateHolder

    /**
     * Register an action listener
     */
    fun addMainThreadActionListener(listener: ActionListener<AD>) {
        mainThreadActionListeners.add(listener)
    }

    /**
     * Remove an action listener
     */
    fun removeMainThreadActionListener(listener: ActionListener<AD>) {
        mainThreadActionListeners.remove(listener)
    }

    /**
     * Dispatches an action to the main thread, no matter from which thread it is called.
     * All action listeners are alerted.
     * @param action an action to be dispatched
     */
    fun dispatch(action: AD) {
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
    private fun alertListenerOnMainThread(action: AD) {
        CoroutineScope(Dispatchers.Main).launch {
            for (listener in mainThreadActionListeners) listener.onAction(action)
        }
    }

    @Deprecated("use plus operator instead")
    operator fun plusAssign(saga: Saga<T, AD, AD>) {
        sagas.add(saga)
    }

    @Deprecated("use plus operator instead")
    operator fun plusAssign(reducer: Reducer<T, AD>) {
        reducers.add(reducer)
    }

    operator fun plus(reducer: Reducer<T, AD>) : Store<T, AD> {
        reducers.add(reducer)
        return this
    }

    operator fun plus(saga: Saga<T, AD, AD>) : Store<T, AD> {
        sagas.add(saga)
        return this
    }

}
