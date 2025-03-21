package ch.smoca.redux.sagas

import ch.smoca.redux.Action
import ch.smoca.redux.State
import kotlin.reflect.KClass

/**
 * A saga that listens to actions and performs long-running or asynchronous operations.
 *
 * Sagas are intended for tasks such as network requests, complex business logic, or other operations
 * that require asynchronous processing. They receive both the old and new state, allowing them to
 * react based on the difference.
 *
 *
 * Sagas are typically initiated by an action and then proceed through multiple processing steps.
 * When used with [CancellableSagaMiddleware], the steps can be canceled if necessary.
 * With [QueueingSagaMiddleware], the subsequent actions are queued until all steps of the
 * preceding action are fully completed.
 *
 * Each saga will be called on its own coroutine with limitedParallelism = 1.
 *
 * To use Sagas,  [CancellableSagaMiddleware] or [QueueingSagaMiddleware] must be provided to the
 * store.
 *
 *
 * **Usage Example:**
 * ```
 * class BusySaga() : Saga<AppState>() {
 *     // A sealed class lets the compiler check if the 'when' expression is exhaustive
 *     sealed class Work: Action {
 *         data object DoWork: Work()
 *     }
 *
 *     override suspend fun onAction(action: Action, oldState: AppState, newState: AppState) {
 *         (action as? Work)?.let {
 *             when (it) {
 *                 Work.DoWork -> {
 *                     // a lof of heavy lifting
 *                   	...
 *                     //the saga has access to the dispatch-function and can dispatch new action
 *                     // that should be processed by a reducer
 *                     dispatch(WorkResult())
 *                 }
 *             }
 *
 *         }
 *     }
 * }
 * ```
 *
 *  @param T the type of state processed by the saga.
 *
 */
abstract class Saga<T : State> {
    lateinit var dispatch: (action: Action) -> Unit

    /**
     * Processes an action asynchronously.
     *
     * This method is invoked when an action is dispatched, and both the state before and after the action are available.
     * It is intended for performing side effects or multi-step asynchronous operations.
     *
     * @param action the dispatched action.
     * @param oldState the state before the action was processed.
     * @param newState the state after the action was processed.
     */
    abstract suspend fun onAction(action: Action, oldState: T, newState: T)

    /**
     * If only certain actions can be processed by the Saga, overwrite [acceptAction]
     * and return the sealed class that defines the action.
     */
    open val acceptAction : KClass<out Action>? = null
}
