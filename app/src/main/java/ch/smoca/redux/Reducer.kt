package ch.smoca.redux

interface Reducer<T : State, A: Any> {
    fun reduce(action: A, state: T): T
}
