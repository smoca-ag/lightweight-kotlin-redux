package ch.smoca.redux.ioc

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import java.lang.IllegalStateException

interface IOCProvider {
    val ioc : IOC

    companion object {
        fun iocOfContext(context: Context?) : IOC {
            //check context for provider
            (context as? IOCProvider)?.let {
                return it.ioc
            }

            //check applicationContext for provider
            (context?.applicationContext as? IOCProvider)?.let {
                return it.ioc
            }

            throw IllegalStateException("NO IOCProvider presend")
        }

        fun iocOfContext(fragment: Fragment): IOC {
            (fragment as? IOCProvider)?.let {
                return it.ioc
            }
            return iocOfContext(fragment.context)
        }

        fun iocOfContext(activity: FragmentActivity): IOC {
            (activity as? IOCProvider)?.let {
                return it.ioc
            }
            return iocOfContext(activity.applicationContext)
        }
    }

}