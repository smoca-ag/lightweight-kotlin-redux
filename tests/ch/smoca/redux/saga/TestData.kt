package ch.smoca.redux.saga

import ch.smoca.redux.Action
import ch.smoca.redux.Saga
import ch.smoca.redux.State
import ch.smoca.redux.sagas.CancellableSagaMiddleware
import ch.smoca.redux.sagas.QueueingSagaMiddleware
import kotlinx.coroutines.delay

class TestSaga : Saga<TestState>() {
    val processedActions = mutableListOf<Action>()
    val startedActions = mutableListOf<Action>()

    sealed class OtherActions : Action {
        data class TestAction(val id: Int = 0) : OtherActions()
    }

    sealed class QueueActions: QueueingSagaMiddleware.QueueingAction {
        data class CancelQueueAction(
            val id: Int = 0,
            override val policy: QueueingSagaMiddleware.Policy = QueueingSagaMiddleware.Policy.CLEAR
        ) : QueueingSagaMiddleware.QueueingAction

        data class AddAction(
            val id: Int = 0,
            override val policy: QueueingSagaMiddleware.Policy = QueueingSagaMiddleware.Policy.ADD
        ) : QueueingSagaMiddleware.QueueingAction
    }

    sealed class CancelledActions : CancellableSagaMiddleware.CancellableAction {
        data class CancellableTestAction(
            val id: Int = 0,
            override val policy: CancellableSagaMiddleware.Policy = CancellableSagaMiddleware.Policy.TAKE_EVERY
        ) : CancelledActions()

        data class QueuedAction(
            val id: Int = 0,
        ) : QueueingSagaMiddleware.QueueingAction

        sealed class SecondLevel : CancelledActions() {
            data object SecondLevelAction : SecondLevel()
        }
    }

    override suspend fun onAction(action: Action, oldState: TestState, newState: TestState) {
        startedActions.add(action)
        delay(1_000)
        processedActions.add(action)
    }
}

data class TestState(val testProperty: Int = 0) : State