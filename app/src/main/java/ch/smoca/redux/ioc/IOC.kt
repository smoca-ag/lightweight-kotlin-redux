package ch.smoca.redux.ioc
import kotlin.reflect.KClass

/**
 *   small component for high level dependency injection
 *
 * @author Oliver Mannhart on 22.02.18 - 16:37
 * @version 0.1
 * @see @see <a href="https://www.smoca.ch">https://www.smoca.ch</a>
 *
 **/


open class IOC{
    private val knownObjects: HashMap<Any, Any> = HashMap()

    /***
     * add an instance to the ICO
     */
    operator fun <T : Any> plusAssign(instance: T) {
        register(instance::class, instance)
    }

    private fun <T : Any> register(key: KClass<T>, instance: Any) {
        knownObjects[key] = instance
    }

    /***
     * returns a registered object. Use resolve() if you dont have the type
     */
    private fun objectOfType(type: Any): Any? {
        return knownObjects[type]
    }

    private fun registeredObjects(): List<Any> {
        return knownObjects.values.toList()
    }


    /***
     * IN KOTLIN USE resolve()
     * Can be used with specific type in java.
     *
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolveType(type: KClass<T>): T {
        var resolvedObject = objectOfType(type) as T?

        if (resolvedObject == null) {
            resolvedObject = registeredObjects().find {
                val a =
                    type.javaObjectType.isAssignableFrom(it::class.java) // could be replaced with isSubClass as soon its available in kotlin
                a
            } as T?

            if (resolvedObject != null) {
                register(type, resolvedObject)
            }
        }
        return resolvedObject ?: throw IllegalAccessError("The Type $type was not registered.")
    }

    inline fun <reified T : Any> resolve(): T {
        // for now we use resolveType. will be refactored as soon as we fully moved to kotlin
        return resolveType(T::class)
    }

}


