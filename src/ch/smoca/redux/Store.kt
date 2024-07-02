package ch.smoca.redux

import ch.smoca.redux.sagas.CancellableSagaMiddleware
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Store for redux like architecture
 * If used with jetpack compose, make sure to mark the store as @Stable
 * @param T the type of your initial state
 * @param initialState the initial state
 */
open class Store<T : State>(
    initialState: T,
    private val reducers: List<Reducer<T>> = emptyList(),
    private val middlewares: List<Middleware<T>> = emptyList(),
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /* convenient constructor that takes sagas and creates a CancellableSagaMiddleware */
    constructor(initialState: T, reducers: List<Reducer<T>>, sagas: List<Saga<T>>, middlewares: List<Middleware<T>> = listOf()) : this(
        initialState,
        reducers,
        middlewares + listOf(CancellableSagaMiddleware(sagas))
    )

    private var state: T = initialState
    private val mainThreadStateListener: MutableList<StateListener> = mutableListOf()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val singleThread = dispatcher.limitedParallelism(1)
    private val stateHolder = MutableStateFlow(state)
    private val internalDispatch: (action: Action) -> Unit

    init {
        internalDispatch = apply()
    }

    fun getState(): T {
        return state
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
            internalDispatch(action)
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

    private fun apply(): (action: Action) -> Unit {
        //root reducers
        val dispatch: (action: Action) -> Unit = { currentAction: Action ->
            reduce(currentAction, this)
        }
        //dispatch for middlewares
        return middlewares.reversed().fold(dispatch) { lastDispatch, middleware ->
            middleware.apply(
                this,
                lastDispatch
            )
        }
    }

    private fun reduce(action: Action, store: Store<T>) {
        val currentState = store.getState()
        this.state =
            reducers.fold(currentState) { preState, reducer -> reducer.reduce(action, preState) }
    }

    // UI state listener will always be notified on the main thread
    private fun alertListenerOnMainThread(state: T) {
        CoroutineScope(Dispatchers.Main).launch {
            for (listener in mainThreadStateListener) listener.onStateChanged(state)
        }
    }

}
