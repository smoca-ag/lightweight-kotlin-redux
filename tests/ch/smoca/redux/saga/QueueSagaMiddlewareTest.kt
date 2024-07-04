package ch.smoca.redux.saga

import ch.smoca.redux.Store
import ch.smoca.redux.sagas.QueueingSagaMiddleware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class QueueSagaMiddlewareTest {

    private lateinit var store: Store<TestState>
    private lateinit var queueingSagaMiddleware: QueueingSagaMiddleware<TestState>
    private lateinit var testSaga: TestSaga

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        testSaga = TestSaga()
        val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())
        queueingSagaMiddleware = QueueingSagaMiddleware(listOf(testSaga), dispatcher)
        store = Store(TestState(), listOf())
        Dispatchers.setMain(dispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testCancelQueue() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        launch {
            (1..3).forEach { i ->
                queueingSagaMiddleware.process(
                    TestSaga.QueueActions.AddAction(i),
                    store
                ) {}
            }

            testScheduler.advanceTimeBy(200)
            queueingSagaMiddleware.process(
                TestSaga.QueueActions.CancelQueueAction(4),
                store
            ) {}
            assertEquals(1, testSaga.startedActions.size, "Only one action should be started")
            assertEquals(
                0,
                testSaga.processedActions.size,
                "No action can be processed at this point"
            )
            testScheduler.advanceTimeBy(10_000)
            assertEquals(0, testSaga.processedActions.size, "All action must be cancelled")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testAddQueue() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        launch {
            (1..3).forEach { i ->
                queueingSagaMiddleware.process(
                    TestSaga.QueueActions.AddAction(i),
                    store
                ) {}
            }
            testScheduler.advanceTimeBy(500)
            //only one action should be started
            assertEquals(1, testSaga.startedActions.size, "Only one action should be started")
            assertEquals(0, testSaga.processedActions.size, "No action can be processed at this point")
            testScheduler.advanceTimeBy(1001)
            // only one should be processed
            assertEquals(1, testSaga.processedActions.size, "Only one action should be processed")
            testScheduler.advanceTimeBy(10_000)
            assertEquals(3, testSaga.processedActions.size, "All action must be processed")
        }
    }

}

