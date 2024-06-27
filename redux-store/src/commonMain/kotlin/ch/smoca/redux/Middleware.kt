package ch.smoca.redux

interface Middleware<T : State> {
    fun process(action: Action, store: Store<T>, nextState: (action: Action, state: T) -> Unit)
    fun apply(
        action: Action,
        store: Store<T>,
        nextState: (action: Action, state: T) -> Unit,
    ): (action: Action, state: T) -> Unit {
        return { currentAction: Action, _: T ->
            process(currentAction, store, nextState)
        }
    }
}