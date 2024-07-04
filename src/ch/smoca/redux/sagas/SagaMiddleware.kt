package ch.smoca.redux.sagas

import ch.smoca.redux.Action
import ch.smoca.redux.Middleware
import ch.smoca.redux.State
import ch.smoca.redux.Store

/*
    * A middleware that processes sagas.
    * The sagas are called after the action has been processed by the store.
    * The sagas are called in the order they are provided.
    * The sagas are called if the action is accepted by the saga (acceptAction).
 */
abstract class SagaMiddleware<T : State>(private val sagas: List<Saga<T>>) : Middleware<T> {
    override fun process(action: Action, store: Store<T>, next: (action: Action) -> Unit) {
        val oldState = store.getState()
        next(action)
        val newState = store.getState()
        sagas.forEach { saga ->
            //only call the saga if the action is accepted
            val acceptAction = saga.acceptAction
            if (acceptAction == null || acceptAction.isInstance(action)
            ) {
                onActionForSaga(saga, action, oldState, newState)
            }
        }
    }

    abstract fun onActionForSaga(
        saga: Saga<T>,
        action: Action,
        oldState: T,
        newState: T
    )
}