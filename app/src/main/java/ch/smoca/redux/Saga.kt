package ch.smoca.redux

abstract class Saga<T : State, A: Any, D: Any>(val dispatch: (action: D) -> Unit) {
    abstract fun onAction(action: A, oldState: T, newState: T)
}
