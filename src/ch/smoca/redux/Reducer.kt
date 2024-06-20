package ch.smoca.redux

interface Reducer<T : State> {
    fun reduce(action: Action, state: T): T
}
