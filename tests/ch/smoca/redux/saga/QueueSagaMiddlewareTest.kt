package ch.smoca.redux.saga

import ch.smoca.redux.Store
import ch.smoca.redux.sagas.QueueingSagaMiddleware
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class QueueSagaMiddlewareTest {


    private lateinit var store: Store<TestState>
    private lateinit var queueingSagaMiddleware: QueueingSagaMiddleware<TestState>
    private lateinit var testSaga: TestSaga

    @BeforeTest
    fun setUp() {
        testSaga = TestSaga()
        queueingSagaMiddleware = QueueingSagaMiddleware(listOf(testSaga))
        store = Store(TestState())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testCancelQueue() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        queueingSagaMiddleware.coroutineDispatcher = dispatcher
        launch {
            (1..3).forEach { i ->
                queueingSagaMiddleware.process(
                    TestSaga.CancelledActions.QueuedAction(i),
                    Store(TestState())
                ) {}
            }

            testScheduler.advanceTimeBy(200)
            queueingSagaMiddleware.process(
                TestSaga.QueueActions.CancelQueueAction(4),
                store
            ) {}
            assertEquals(1, testSaga.startedActions.size)
            assertEquals(0, testSaga.processedActions.size)
            testScheduler.advanceTimeBy(10_000)
            assertEquals(0, testSaga.processedActions.size)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testAddQueue() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        queueingSagaMiddleware.coroutineDispatcher = dispatcher
        launch {
            (1..3).forEach { i ->
                queueingSagaMiddleware.process(
                    TestSaga.QueueActions.AddAction(i),
                    Store(TestState())
                ) {}
            }
            testScheduler.advanceTimeBy(500)
            //only one action should be started
            assertEquals(1, testSaga.startedActions.size)
            assertEquals(0, testSaga.processedActions.size)
            testScheduler.advanceTimeBy(1001)
            // only one should be processed
            assertEquals(1, testSaga.processedActions.size)
            testScheduler.advanceTimeBy(10_000)
            assertEquals(3, testSaga.processedActions.size)
        }
    }

}

