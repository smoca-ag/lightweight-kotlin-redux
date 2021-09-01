package ch.smoca.redux.ui
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.asLiveData
import ch.smoca.redux.Action
import ch.smoca.redux.State
import ch.smoca.redux.Store
import ch.smoca.redux.ioc.IOCProvider

abstract class ReduxFragment<S: State> : Fragment() {

    private lateinit var  store: Store<S>
    val state: S
        get() = store.stateObservable.value

    override fun onCreate(savedInstanceState: Bundle?) {
        store = IOCProvider.iocOfContext(this).resolve()
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        store.stateObservable.asLiveData().observe(this.viewLifecycleOwner::getLifecycle) {
            onStateChanged(it)
        }
    }

    fun dispatch(action: Action) {
        store.dispatch(action)
    }
    abstract fun onStateChanged(state: S)

}