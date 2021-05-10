package ch.smoca.redux.ui
import android.os.Bundle
import androidx.fragment.app.Fragment
import ch.smoca.redux.Action
import ch.smoca.redux.State
import ch.smoca.redux.Store
import ch.smoca.redux.ioc.IOCProvider
import java.lang.IllegalStateException

abstract class ReduxFragment<S: State> : Fragment() {

    private lateinit var  store: Store<S>
    val state: S
        get() = store.stateObservable.value ?: throw IllegalStateException("Store does not return state")

    override fun onCreate(savedInstanceState: Bundle?) {
        store = IOCProvider.iocOfContext(this).resolve()
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        store.stateObservable.observe(this.viewLifecycleOwner::getLifecycle) {
            onStateChanged(it)
        }
    }

    fun dispatch(action: Action) {
        store.dispatch(action)
    }
    abstract fun onStateChanged(state: S)

}