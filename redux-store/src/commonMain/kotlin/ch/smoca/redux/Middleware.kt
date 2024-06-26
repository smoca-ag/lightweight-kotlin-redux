package ch.smoca.redux

interface Middleware<T : State> {
    fun process(action: Action, state: T, nextState: (action: Action, state: T) -> T, dispatch: (action: Action) -> Unit): T
    fun apply(
        action: Action,
        state: T,
        nextState: (action: Action, state: T) -> T,
        dispatch: (action: Action) -> Unit
    ): (action: Action, state: T) -> T {
        return { currentAction: Action, currentState: T ->
            process(currentAction, currentState, nextState, dispatch)
        }
    }
}