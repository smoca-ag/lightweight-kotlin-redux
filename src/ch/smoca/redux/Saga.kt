package ch.smoca.redux

abstract class Saga<T : State> {
    lateinit var dispatch: (action: Action) -> Unit
    abstract suspend fun onAction(action: Action, oldState: T, newState: T)
}


