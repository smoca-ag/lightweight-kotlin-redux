package ch.smoca.redux

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext

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
    private val middlewares: MutableList<Middleware<T>> = mutableListOf()
    private val reducers: MutableList<Reducer<T>> = mutableListOf()

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    private val singleThread = newSingleThreadContext("redux-dispatcher")
    private val stateHolder = MutableStateFlow(state)

    fun addReducer(reducer: Reducer<T>) {
        reducers.add(reducer)
    }

    fun addSaga(saga: Saga<T>) {
        sagas.add(saga)
    }

    fun addMiddleware(middleware: Middleware<T>) {
        this.middlewares.add(middleware)
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
    @OptIn(ExperimentalCoroutinesApi::class)
    fun dispatch(action: Action) {
        CoroutineScope(singleThread).launch {
            val oldState = state
            state = apply(action, state)
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

    private fun apply(action: Action, state: T): T {
        //dispatch for reducers
        var dispatch = { currentAction: Action, currentState: T ->
            reduce(currentAction, currentState)
        }
        //dispatch for middlewares
        dispatch = middlewares.reversed().fold(dispatch) { lastDispatch, middleware ->
            middleware.apply(
                action,
                state,
                lastDispatch,
                this::dispatch
            )
        }
        //dispatch for sagas (always included). The SagaMiddleware will called before every other saga.
        dispatch = applySagaMiddleware(action, state, dispatch)

        return dispatch(action, state)
    }

    private fun applySagaMiddleware(action: Action, state: T, dispatch: (action: Action, T) -> T): (action: Action, state: T) -> T {
        return object : Middleware<T> {
            override fun process(
                action: Action,
                state: T,
                nextState: (action: Action, state: T) -> T,
                dispatch: (action: Action) -> Unit
            ): T {
                val newState = nextState(action, state)
                sagas.forEach { saga -> saga.onAction(action, state, newState) }
                return newState
            }
        }.apply(action, state, dispatch, this::dispatch)
    }

    private fun reduce(action: Action, state: T): T {
        return reducers.fold(state) { preState, reducer -> reducer.reduce(action, preState) }
    }

    // UI state listener will always be notified on the main thread
    private fun alertListenerOnMainThread(state: T) {
        CoroutineScope(Dispatchers.Main).launch {
            for (listener in mainThreadStateListener) listener.onStateChanged(state)
        }
    }
}
