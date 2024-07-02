package ch.smoca.redux.sagas

import ch.smoca.redux.Action
import ch.smoca.redux.State

abstract class Saga<T : State> {
    lateinit var dispatch: (action: Action) -> Unit
    abstract suspend fun onAction(action: Action, oldState: T, newState: T)
}


