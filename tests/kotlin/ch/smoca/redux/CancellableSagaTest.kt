import ch.smoca.redux.Action
import ch.smoca.redux.Saga
import ch.smoca.redux.State
import ch.smoca.redux.Store
import ch.smoca.redux.sagas.CancellableSagaMiddleware
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CancellableSagaTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testTakeEvery() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testSaga = TestSaga()

        launch {
            val middleware = CancellableSagaMiddleware(listOf(testSaga), testDispatcher)
            repeat((1..3).count()) {
                middleware.process(CancellableTestAction(), Store<TestState>(TestState(0))) {}
            }
        }
        //the test saga waits for 1000ms.
        testScheduler.advanceTimeBy(1001)
        //3 actions should go trough the.
        assertEquals(3, testSaga.processSteps)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testTakeLatest() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testSaga = TestSaga()

        launch {
            val middleware = CancellableSagaMiddleware(listOf(testSaga), testDispatcher)
            (1..3).forEachIndexed() { index, _ ->
                middleware.process(
                    CancellableTestAction(id = index + 1, policy = CancellableSagaMiddleware.Policy.TAKE_LATEST),
                    Store<TestState>(TestState(0))
                ) {}
            }

        }
        //the test saga waits for 1000ms. But only the last action should go trough the.
        testScheduler.advanceTimeBy(1001)
        //only the last action should go trough the.
        assertEquals(1, testSaga.processSteps, "Only last action should go trough")
        //the last action should have id 3.
        assertEquals(3, testSaga.lastId, "Id of last Action should be 3")
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun takeLeading() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testSaga = TestSaga()

        launch {
            val middleware = CancellableSagaMiddleware(listOf(testSaga), testDispatcher)
            (1..3).forEachIndexed() { index, _ ->
                middleware.process(
                    CancellableTestAction(
                        id = index + 1,
                        policy = CancellableSagaMiddleware.Policy.TAKE_LEADING
                    ),
                    Store(TestState(0))
                ) {}
            }
        }
        //the test saga waits for 1000ms. But only the last action should go trough the.
        testScheduler.advanceTimeBy(1001)
        //only the last action should go trough the.
        assertEquals(1, testSaga.processSteps)
        //the first action should have id 1.
        assertEquals(1, testSaga.lastId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancel() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testSaga = TestSaga()

        launch {
            val middleware = CancellableSagaMiddleware(listOf(testSaga), testDispatcher)
            (1..3).forEachIndexed() { index, _ ->
                middleware.process(
                    CancellableTestAction(
                        id = index + 1,
                        policy = CancellableSagaMiddleware.Policy.CANCEL
                    ),
                    Store(TestState(0))
                ) {}
            }
        }
        //the test saga waits for 1000ms. But all action should be canceled
        testScheduler.advanceTimeBy(1001)
        //no cation should go trough
        assertEquals(0, testSaga.processSteps)
    }



}





data class CancellableTestAction(
    val id: Int = 0,
    override val policy: CancellableSagaMiddleware.Policy = CancellableSagaMiddleware.Policy.TAKE_EVERY
) : CancellableSagaMiddleware.CancellableAction


class TestSaga(var processSteps: Int = 0, var lastId: Int = -1) : Saga<TestState>() {
    override suspend fun onAction(action: Action, oldState: TestState, newState: TestState) {
        delay(1_000)
        processSteps++
        lastId = (action as? CancellableTestAction)?.id ?: -1
    }
}

data class TestState(val number: Int) : State