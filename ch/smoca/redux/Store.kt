package ch.smoca.redux

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

class Store(initialState: State) {

    private var state: State = initialState
    private val stateHolder: MutableLiveData<State>
    private val mainThreadActionListeners: MutableList<ActionListener> = mutableListOf()
    private val sagas: MutableList<Saga> = mutableListOf()
    private val reducers: MutableList<Reducer> = mutableListOf()
    private val singleThread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    init {
        stateHolder = MutableLiveData(state)
    }

    // Will return a live data to observe state change
    val stateObservable: LiveData<State>
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
            sagas.forEach { it.onAction(action, state) }
            val oldState = state
            state = reducers.fold(state) { preState, reducer -> reducer.reduce(action, preState) }

            if (state != oldState) {
                // we will not post the state if it did not change.
                // however, it is still possible that the UI receives the same state twice.
                // if something changes value fast to something and back, the UI thread may
                // receive the first state and the last state.
                stateHolder.postValue(state)
            }

            alertListenerOnMainThread(action)
        }
    }

    // UI Action Listener will always be notified on the main thread. For every action
    private fun alertListenerOnMainThread(action: Action) {
        CoroutineScope(Dispatchers.Main).launch {
            mainThreadActionListeners.forEach { it.onAction(action) }
        }
    }

    operator fun plusAssign(saga: Saga) {
        sagas.add(saga)
    }

    operator fun plusAssign(reducer: Reducer) {
        reducers.add(reducer)
    }
}

abstract class Saga(val dispatch: (action: Action) -> Unit) {
    abstract fun onAction(action: Action, state: State)
}

interface Reducer {
    fun reduce(action: Action, state: State): State
}

interface ActionListener {
    fun onAction(action: Action)
}

interface State
interface Action
