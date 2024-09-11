package ch.smoca.redux.stateobservers

import ch.smoca.redux.Action
import ch.smoca.redux.State

abstract class StateObserver<T : State> {
    lateinit var dispatch: (action: Action) -> Unit
    abstract fun onStateChanged(state: T)
}