package ch.smoca.redux

interface Middleware<T : State> {
    //process the action
    fun process(action: Action, store: Store<T>, next: (action: Action) -> Unit)
    //apply the middleware to the store
    fun apply(
        store: Store<T>,
        next: (action: Action) ->  Unit,
    ): (action: Action) -> Unit {
        return { currentAction: Action ->
            process(currentAction, store, next)
        }
    }
}