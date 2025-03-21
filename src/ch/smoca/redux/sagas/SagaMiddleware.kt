package ch.smoca.redux.sagas

import ch.smoca.redux.Action
import ch.smoca.redux.Middleware
import ch.smoca.redux.State
import ch.smoca.redux.Store

/**
 * A middleware that processes sagas after the store has processed an action.
 *
 * The sagas are executed in the order they are provided and only if the action is accepted by the saga
 * (i.e. if [Saga.acceptAction] is either null or matches the action type). Additionally, each saga is injected
 * with the dispatch function of the store so that they can dispatch new actions if needed.
 *
 * @param T the type of state handled by the store and the sagas.
 * @property sagas the list of sagas to be processed.
 */
abstract class SagaMiddleware<T : State>(private val sagas: List<Saga<T>>) : Middleware<T> {

    /**
     * Applies the middleware to the store.
     */
    override fun apply(
        store: Store<T>,
        next: (action: Action) -> Unit,
    ): (action: Action) -> Unit {
        sagas.forEach { saga ->
            saga.dispatch = store::dispatch
        }
        return super.apply(store, next)
    }

    /**
     * Processes an action by first letting the store update its state, then notifying the sagas.
     *
     * The method captures the state before and after the action is processed. For each saga,
     * if the saga accepts the action (determined by [Saga.acceptAction]), the abstract method
     * [onActionForSaga] is called to delegate further processing.
     *
     * @param action the dispatched action.
     * @param store the store holding the current state.
     * @param next a function that passes the action to the next middleware or reducer.
     */
    override fun process(action: Action, store: Store<T>, next: (action: Action) -> Unit) {
        val oldState = store.getState()
        next(action)
        val newState = store.getState()
        sagas.forEach { saga ->
            val acceptAction = saga.acceptAction
            if (acceptAction == null || acceptAction.isInstance(action)
            ) {
                onActionForSaga(saga, action, oldState, newState)
            }
        }
    }

    /**
     * Called to process an action for a specific saga.
     *
     * This abstract method must be implemented by subclasses to define how an action should be
     * handled by the saga. It receives the saga, the action, and both the old and new state.
     *
     * @param saga the saga that will process the action.
     * @param action the dispatched action.
     * @param oldState the state before the action was processed.
     * @param newState the state after the action was processed.
     */
    abstract fun onActionForSaga(
        saga: Saga<T>,
        action: Action,
        oldState: T,
        newState: T,
    )
}