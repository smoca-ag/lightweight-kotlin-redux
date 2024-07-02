package ch.smoca.redux.saga


import ch.smoca.redux.Store
import ch.smoca.redux.saga.TestSaga.CancelledActions
import ch.smoca.redux.sagas.CancellableSagaMiddleware
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CancellableSagaTest {

    private lateinit var testSaga: TestSaga
    private lateinit var cancellableSagaMiddleware: CancellableSagaMiddleware<TestState>
    private lateinit var store: Store<TestState>
    @BeforeTest
    fun setUp() {
        testSaga = TestSaga()
        cancellableSagaMiddleware = CancellableSagaMiddleware(listOf(testSaga))
        store = Store(TestState(), listOf())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testTakeEvery() = runTest {
        cancellableSagaMiddleware.coroutineDispatcher = StandardTestDispatcher(testScheduler)
        launch {
            (1..3).forEach { i ->
                cancellableSagaMiddleware.process(
                    TestSaga.CancelledActions.CancellableTestAction(i),
                    store
                ) {}
            }
            //the test saga waits for 1000ms.
            testScheduler.advanceTimeBy(3000)
            //3 actions should go through.
            assertEquals(3, testSaga.processedActions.size)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testTakeLatest() = runTest {
        cancellableSagaMiddleware.coroutineDispatcher = StandardTestDispatcher(testScheduler)
        launch {
            (1..3).forEachIndexed() { index, _ ->
                cancellableSagaMiddleware.process(
                    TestSaga.CancelledActions.CancellableTestAction(
                        id = index + 1,
                        policy = CancellableSagaMiddleware.Policy.TAKE_LATEST
                    ),
                    store
                ) {}
            }
        }
        //the test saga waits for 1000ms. But only the last action should go through.
        testScheduler.advanceTimeBy(1001)
        //only the last action should go through.
        assertEquals(1, testSaga.processedActions.size, "Only 1 action should go trough")
        //the last action must have id 3.
        assertEquals(
            3,
            (testSaga.processedActions.first() as TestSaga.CancelledActions.CancellableTestAction).id,
            "last action must have id 3"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testTakeLeading() = runTest {
        cancellableSagaMiddleware.coroutineDispatcher = StandardTestDispatcher(testScheduler)
        launch {
            (1..3).forEachIndexed() { index, _ ->
                cancellableSagaMiddleware.process(
                    TestSaga.CancelledActions.CancellableTestAction(
                        id = index + 1,
                        policy = CancellableSagaMiddleware.Policy.TAKE_LEADING
                    ),
                    store
                ) {}
            }
        }
        //the test saga waits for 1000ms. But only the first action should go through.
        testScheduler.advanceTimeBy(2000)
        //only the first action should go through.
        assertEquals(1, testSaga.processedActions.size, "Only 1 action should go through")
        //the action must have id 1.
        assertEquals(
            1,
            (testSaga.processedActions.first() as TestSaga.CancelledActions.CancellableTestAction).id,
            "the action must have id 1"
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testCancel() = runTest {
        cancellableSagaMiddleware.coroutineDispatcher = StandardTestDispatcher(testScheduler)
        launch {
            (1..3).forEachIndexed() { index, _ ->
                cancellableSagaMiddleware.process(
                    TestSaga.CancelledActions.CancellableTestAction(
                        id = index + 1,
                        policy = CancellableSagaMiddleware.Policy.CANCEL_LAST
                    ),
                    store
                ) {}
            }
        }
        //the test saga waits for 1000ms. But all action should be canceled
        testScheduler.advanceTimeBy(1001)
        //no action should go through
        assertEquals(0, testSaga.processedActions.size, "No action should go through")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testFilterActions() = runTest {
        cancellableSagaMiddleware.coroutineDispatcher = StandardTestDispatcher(testScheduler)
        cancellableSagaMiddleware.acceptedActions = mapOf(
            testSaga to CancelledActions::class
        )
        launch {
            cancellableSagaMiddleware.process(
                TestSaga.OtherActions.TestAction(
                    id = 0
                ),
                store
            ) {}

            cancellableSagaMiddleware.process(
                TestSaga.CancelledActions.CancellableTestAction(
                    id = 0,
                    policy = CancellableSagaMiddleware.Policy.TAKE_EVERY
                ),
                store
            ) {}

            cancellableSagaMiddleware.process(
                TestSaga.CancelledActions.SecondLevel.SecondLevelAction,
                store
            ) {}
        }
        testScheduler.advanceTimeBy(1001)
        //only cancellable actions should be processed
        assertEquals(
            1,
            testSaga.processedActions.filterIsInstance<TestSaga.CancelledActions.CancellableTestAction>().size,
            "CancellableTestAction must be processed"
        )
        assertEquals(
            1,
            testSaga.processedActions.filterIsInstance<TestSaga.CancelledActions.SecondLevel.SecondLevelAction>().size,
            "SecondLevel Action must be processed"
        )
        assertEquals(2, testSaga.processedActions.size, "Only two actions should be processed")
    }
}



