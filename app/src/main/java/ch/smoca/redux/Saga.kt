package ch.smoca.redux

abstract class Saga<T : State, A: Any>(val dispatch: (action: A) -> Unit) {
    abstract fun onAction(action: A, oldState: T, newState: T)
}
