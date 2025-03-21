package ch.smoca.redux.sagas

import ch.smoca.redux.Action
import ch.smoca.redux.State
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * A middleware that queues actions and processes them sequentially in a saga.
 *
 * This middleware is designed for scenarios where actions should be processed in order.
 * Actions that are not instances of [QueueingAction] are processed with the [ADD][Policy.ADD] Policy.
 *
 * @param T the type of state processed by the sagas.
 * @property coroutineDispatcher the dispatcher on which the queued actions are processed (default is [Dispatchers.IO]).
 *
 * @constructor Creates a new instance of [QueueingSagaMiddleware] with the provided sagas.
 */
class QueueingSagaMiddleware<T : State>(
    sagas: List<Saga<T>>,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SagaMiddleware<T>(sagas) {

    /**
     * Defines the policy for queuing actions.
     *
     * Policies dictate how incoming actions are added to or removed from the queue.
     */
    enum class Policy {
        /**
         * Adds the action to the queue.
         */
        ADD,

        /**
         * Clears the queue, discarding all queued actions.
         */
        CLEAR,

        /**
         * Clears the queue and then adds the action.
         */
        CLEAR_AND_ADD,
    }

    /**
     * Interface for actions that support queueing policies.
     *
     * Implementing this interface allows an action to specify its desired behavior regarding queue management.
     * By default, the [policy] is set to [ADD][Policy.ADD].
     */
    interface QueueingAction : Action {
        val policy: Policy
            get() = Policy.ADD
    }

    /**
     * A data class representing an action that has been queued for processing.
     *
     * It bundles the [action] with the state before and after the action.
     *
     * @param T the type of state.
     * @property action the dispatched action.
     * @property oldState the state before the action was processed.
     * @property newState the state after the action was processed.
     */
    data class QueuedAction<T : State>(val action: Action, val oldState: T, val newState: T) :
        QueueingAction

    /**
     * Holds the queue and the consumer job for a saga.
     *
     * @param T the type of state.
     * @property queue A [Channel] that holds [QueuedAction] instances.
     * @property consumer the job that consumes the actions from the queue.
     */
    data class SagaQueue<T : State>(
        var queue: Channel<QueuedAction<T>> = Channel(Channel.UNLIMITED),
        var consumer: Job? = null,
    )

    private val contexts: MutableMap<Saga<T>, SagaQueue<T>> = mutableMapOf()


    override fun onActionForSaga(saga: Saga<T>, action: Action, oldState: T, newState: T) {
        val sagaQueue = contexts[saga] ?: SagaQueue()
        contexts[saga] = sagaQueue
        if (processToQueue(sagaQueue, (action as? QueueingAction)?.policy ?: Policy.ADD)) {
            CoroutineScope(coroutineDispatcher).launch {
                sagaQueue.queue.send(QueuedAction(action, oldState, newState))
            }

            //start consumer
            if (sagaQueue.consumer == null) {
                sagaQueue.consumer = CoroutineScope(coroutineDispatcher).launch {
                    for (queuedAction in sagaQueue.queue) {
                        saga.onAction(
                            queuedAction.action,
                            queuedAction.oldState,
                            queuedAction.newState
                        )
                    }
                }

            }
        }
    }

    /**
     * @return true if the new elements can be added
     */
    private fun processToQueue(
        sagaQueue: SagaQueue<T>,
        policy: Policy,
    ): Boolean {
        return when (policy) {
            Policy.ADD -> {
                true
            }

            Policy.CLEAR -> {
                //clear everything
                clearSagaQueue(sagaQueue)
                false
            }

            Policy.CLEAR_AND_ADD -> {
                clearSagaQueue(sagaQueue)
                true
            }
        }
    }

    private fun clearSagaQueue(sagaQueue: SagaQueue<T>) {
        sagaQueue.queue.cancel()
        sagaQueue.queue = Channel(Channel.UNLIMITED)
        sagaQueue.consumer?.cancel()
        sagaQueue.consumer = null
    }


}