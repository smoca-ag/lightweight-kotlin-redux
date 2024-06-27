package ch.smoca.redux

interface Middleware<T : State> {
    fun process(action: Action, store: Store<T>, next: (action: Action) -> Unit)
    fun apply(
        store: Store<T>,
        next: (action: Action) ->  Unit,
    ): (action: Action) -> Unit {
        return { currentAction: Action ->
            process(currentAction, store, next)
        }
    }
}