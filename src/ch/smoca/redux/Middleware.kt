package ch.smoca.redux

/**
 * A middleware that intercepts actions before they reach the reducer.
 *
 * Middlewares are used to log actions, perform side effects, modify actions, or even cancel them.
 * They have access to the store and can pass the action further down the chain by invoking the [next] function.
 *
 * In other Redux implementation, a middleware can return a value.
 * This is not supported in this implementation, since `dispatch(action)` runs on a different thread and can not return anything.
 *
 * **Usage Example**
 * ```
 * class LogMiddleware : Middleware<AppState> {
 *     override fun process(
 *         action: Action,
 *         store: Store<AppState>,
 *         next: (action: Action) -> Unit
 *     ) {
 *         // read the current state from the store
 *         val currentState = store.getState()
 *
 *         // next(action) will pass the action to the next middleware in the chain.
 *         // If next is not called, the action is aborted.
 *         val result = next(action)
 *
 *         // After the call to next, the action is reduced into the state,
 *         // if no other middleware further down the road cancels it
 *         val newState = store.getState()
 *         Log.d(
 *             this::class.simpleName,
 *             "Diff:\n" +
 *             "Action:\n$action\n" +
 *             "Old:\n$currentState\n" +
 *             "New:\n$newState"
 *         )
 *     }
 * }
 * ```
 *
 * @param T The type of state that the middleware processes.
 */
interface Middleware<T : State> {
    /**
     * Processes an action prior to its reduction.
     *
     * This method can alter, cancel, or pass the action along the middleware chain. If the action
     * should continue processing, the middleware must invoke [next] with the action.
     *
     * @param action The action to process.
     * @param store The store that holds the state.
     * @param next A function that forwards the action to the next middleware or the reducer.
     */
    fun process(action: Action, store: Store<T>, next: (action: Action) -> Unit)

    /**
     * Apply the middleware to the store
     */
    fun apply(
        store: Store<T>,
        next: (action: Action) ->  Unit,
    ): (action: Action) -> Unit {
        return { currentAction: Action ->
            process(currentAction, store, next)
        }
    }
}