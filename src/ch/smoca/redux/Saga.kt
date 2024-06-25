package ch.smoca.redux

abstract class Saga<T : State>(val dispatch: (action: Action) -> Unit) {
    abstract fun onAction(action: Action, oldState: T, newState: T)
}
