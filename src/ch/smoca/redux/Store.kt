package ch.smoca.redux

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
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
    sagas: List<Saga<T>> = emptyList(),
    private val middlewares: List<Middleware<T>> = emptyList()
) {
    private var state: T = initialState
    private val mainThreadStateListener: MutableList<StateListener> = mutableListOf()
    private lateinit var sagas: List<Pair<Saga<T>, CoroutineDispatcher>>

    @OptIn(ExperimentalCoroutinesApi::class)
    private val singleThread = Dispatchers.IO.limitedParallelism(1)
    private val stateHolder = MutableStateFlow(state)
    private val internalDispatch: (action: Action) -> Unit

    init {
        addSagas(sagas)
        internalDispatch = apply()
    }

    fun getState(): T {
        return state
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun addSagas(initSagas: List<Saga<T>>) {
        // create a dispatcher view for each saga
        sagas = initSagas.map { saga ->
            saga.dispatch = this::dispatch
            Pair(saga, Dispatchers.IO.limitedParallelism(1))
        }
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
        var dispatch: (action: Action) -> Unit = { currentAction: Action ->
            reduce(currentAction, this)
        }
        //dispatch for middlewares
        dispatch = middlewares.reversed().fold(dispatch) { lastDispatch, middleware ->
            middleware.apply(
                this,
                lastDispatch
            )
        }
        //dispatch for sagas (always included). The SagaMiddleware will called before every other saga.
        return applySagaMiddleware(dispatch)
    }

    private fun applySagaMiddleware(
        dispatch: (action: Action) -> Unit
    ): (action: Action) -> Unit {
        return object : Middleware<T> {
            override fun process(action: Action, store: Store<T>, next: (action: Action) -> Unit) {
                val oldState = store.getState()
                next(action)
                val newState = store.getState()
                sagas.forEach { sagaContext ->
                    val saga = sagaContext.first
                    val coroutineDispatcher = sagaContext.second
                    CoroutineScope(coroutineDispatcher).launch {
                        saga.onAction(action, oldState, newState)
                    }
                }
            }
        }.apply(this, dispatch)
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
