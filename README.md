# How To Use

This library can be used for two scenarios which differ in creation:

- [Project Setup](#project-setup)
  + [KMP State Definition](#kmp-state-definition)
  + [Native Implementation](#native-implementation)
    - [iOS](#ios)
    - [Android](#android) 
- [Create Redux Components](#create-redux-components)

# Project Setup

## KMP State Definition
1. Create a repository in GitLab. This repository will be used for the State definition (KMP), Android Native, and iOS Native.

2. Create three folders: `mkdir android ios multiplatform`

3. In Android Studio add the multiplatform IDE Plugin `Android Studio > Settings > Plugins > Marketplace > Kotlin Multiplatform`

4. Create a new Kotlin Multiplatform Library Project called `{ProjectName}State` and use the folder `multiplatform`

5. Open `gradle/libs.versions.toml` and add these dependencies (versions might be updated):
```diff
[versions]
agp = "8.5.0"
kotlin = "2.0.0"
+coroutines = "1.9.0-RC"
+serialization = "2.0.0"
+redux = "6.0.0"
+ktor = "2.3.11" # only needed for the network example

[libraries]
+kotlin-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
+kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
+ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
+ktor-client-serialization-core = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
+ktor-client-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
+ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
+ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
+smoca-redux = { module = "ch.smoca.lib:lightweight-kotlin-redux", version.ref = "redux" }

[plugins]
+serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "serialization" }
androidLibrary = { id = "com.android.library", version.ref = "agp" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
```

6. Open `shared/build.gradle.kts` and apply serialization plugin:
```diff
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
+   alias(libs.plugins.serialization)
}
```

7. In the same file configure the iOS Framework building:
```diff
kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
        }
    }

+    val xcFrameworkName = "Redux"
+    val xcf = XCFramework(xcFrameworkName)

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
+            baseName = xcFrameworkName
+            binaryOption("bundleId", "ch.smoca.${xcFrameworkName}")
+            xcf.add(this)
            isStatic = true
        }
    }
}
```

8. Last to do in this file is adding the defined dependencies:
```diff
kotlin {
    sourceSets {
        commonMain.dependencies {
-            //put your multiplatform dependencies here
+            implementation(libs.smoca.redux)
+            implementation(libs.kotlin.coroutines.core)
+            implementation(libs.ktor.client.core)
+            implementation(libs.ktor.client.serialization.core)
+            implementation(libs.ktor.client.serialization.json)
+        }
+        androidMain.dependencies {
+            implementation(libs.ktor.client.okhttp)
+            implementation(libs.kotlinx.coroutines.android)
+        }
+        iosMain.dependencies {
+            implementation(libs.ktor.client.darwin)
+        }
-        commonTest.dependencies {
-            implementation(libs.kotlin.test)
-        }
    }
}
```

9. The Project should now sync and build successfully. Continue by [creating the Redux components](#create-redux-components)

## Native Implementation
After finishing your Redux components this project must now be imported to our native environment.

### iOS
In gradle we defined the iOS output to be an xcFramework. This can be built with the following gradle command:
```bash
# ./gradlew :{sharedModuleName}:assemble{xcFrameworkName}XCFramework
# example: 
./gradlew :shared:assembleReduxXCFramework
```

This will create an output in `{sharedModuleName}/build/XCFrameworks/release/Redux.xcframwork`

In your iOS App open `Build Target > General`. Then add the just created Framework folder `Frameworks, Libraries, and Embedded Content > + > Add Other... > Add Files...`. This works smoothly because iOS and the KMP Project are in the same repository.

Because we do not want to execute this command all the time something changed in the KMP Project it is best practice to add this command to the build phase.

The store should be created in the SceneDelegate: 
```swift
import Redux
import UIKit

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        guard let windowScene = (scene as? UIWindowScene) else { return }

        let window = UIWindow(windowScene: windowScene)
        let store = AppStore.companion.setup()
        window.rootViewController = StartViewController(store: store)
        self.window = window
        window.makeKeyAndVisible()
    }
}
```

Because Swift cannot natively use Kotlin Flows and observe them, this library also implements the observer pattern. Like this, swift components can easily subscribe to state changes:
```swift
class SwiftStateListener: Redux_storeStateListener {
    @objc(onStateChangedState:) func onStateChanged(state: Any?) {
        let appState = state as? AppState
    }

    var store: AppStore

    init(store: AppStore) {
        self.store = store
        store.addStateListener(listener: self)
    }
}
```

### Android
For Android there is no need to build a binary because the library runs on Kotlin. Here we need to import the source code of the multiplatform as module. This is a bit more tricky, because we need to pay attention to include all dependencies.

1. Open `settings.gradle.kts` and add these modules:
```diff
include(":redux")
project(":redux").projectDir = File("../multiplatform/redux-store/kmp/redux")
include(":shared")
project(":shared").projectDir = File("../multiplatform/shared")
```

2. Open `app/build.gradle.kts` to implement the added modules:
```diff
dependencies {
+    implementation(project(":shared"))
+    implementation(project(":redux"))
}
```

3. Open `gradle/libs.versions.toml` and add all needed dependencies:
```diff
[versions]
agp = "8.5.0"
kotlin = "2.0.0"
+coroutines = "1.9.0-RC"
+ktor = "2.3.11"
+serialization = "2.0.0"

[libraries]
+kotlin-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
+kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
+ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
+ktor-client-serialization-core = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
+ktor-client-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
+ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
+ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }

[plugins]
+serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "serialization" }
+androidLibrary = { id = "com.android.library", version.ref = "agp" }
+kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
```

4.. An Android Project does not have KMP support so we need to apply some plugins.

`build.gradle.kts`
```diff
plugins {
+    alias(libs.plugins.androidLibrary) apply false
+    alias(libs.plugins.kotlinMultiplatform) apply false
}
```
 
`app/build.gradle.kts`
```diff
plugins {
+    alias(libs.plugins.android.application)
+    alias(libs.plugins.jetbrains.kotlin.android)
}
```

After all of this is done, the project should be able to sync and build. The Store should be initialized when the Application is created:

```kotlin
import android.app.Application
import ch.smoca.demo.AppStore

class MainApplication: Application() {
    lateinit var store: AppStore

    override fun onCreate() {
        super.onCreate()
        store = AppStore.setup()
    }
}
```

Best practice is to access state changes on Android is the `observableState` Flow:
```kotlin

class MainActivity : ComponentActivity() {
    private lateinit var store: AppStore

    override fun onCreate(savedInstanceState: Bundle?) {
        val app = application as MainApplication
        store = app.store

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by store.stateObservable.collectAsState()
            TestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Text(
                        text = state.person.name,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
```

# Create Redux Components
Here a short description of all components and what they do:
* Store
  - Holds the observable State
  - Informs listeners of State changes
  - Gets incoming actions (dispatch)
  - Passes Action and current State to Reducers
  - Passes Action, dispatch function, Old State and New State to Sagas
* Reducer
  - Can listen to Actions
  - Manipulates the State
* Sagas (middleware)
  - Can listen to Actions
  - Has access to old and new State
  - Can dispatch Actions

1. In your source directory create a structure like this:
```
.
├── reducers/
│   └── PersonReducer.kt --> example
├── sagas/
│   └── NetworkSaga.kt   --> example
├── AppState.kt
└── AppStore.kt
```

2. Open `AppState.kt` to and create your State. Here an Example:
```kotlin
import ch.smoca.redux.State
import kotlinx.serialization.Serializable

@Serializable
data class AppState(
    val person: Person = Person(),
): State

@Serializable
data class Person(
    val name: String = "Mock"
)
```

3. Create your reducers in `reducers`. Here a simple example:
```kotlin
import ch.smoca.demo.AppState
import ch.smoca.redux.Action
import ch.smoca.redux.Reducer

class PersonReducer : Reducer<AppState> {
    sealed class PersonAction : Action {
        data class UpdateName(val name: String) : PersonAction()
    }

    override fun reduce(action: Action, state: AppState): AppState {
        val personAction = action as? PersonAction ?: return state

        return when (personAction) {
            is PersonAction.UpdateName -> state.copy(person = state.person.copy(firstname = personAction.firstname))
        }
    }
}
```

4. Create your sagas in `sagas`. Here a simple example:
```kotlin
import ch.smoca.demo.reducers.PersonReducer
import ch.smoca.demo.Person
import ch.smoca.redux.Action
import ch.smoca.redux.Saga
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

class NetworkSaga(dispatch: (action: Action) -> Unit) : Saga<AppState>(dispatch) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    var baseUrl: String = "https://api.person.lol/"

    sealed class NetworkAction : Action {
        data class FetchPerson(val endpoint: String = "1") : NetworkAction()
    }

    override fun onAction(action: Action, oldState: AppState, newState: AppState) {
        val networkAction = action as? NetworkAction ?: return

        when (networkAction) {
            is NetworkAction.FetchPerson -> getPerson(networkAction)
        }
    }

    private fun getPerson(action: NetworkAction.FetchCatFact) {
        CoroutineScope(Dispatchers.IO).launch {
            val url = "$baseUrl${action.endpoint}"
            val response = client.get(url)
            if (response.status.value in 200..299) {
                val person: Person = response.body()
                dispatch(PersonReducer.PersonAction.UpdateName(person.name))
            }
        }
    }
}
```

5. Open `AppStore.kt` to create a setup function for the native platforms to call:
```kotlin
import ch.smoca.demo.reducers.PersonReducer
import ch.smoca.demo.sagas.NetworkSaga
import ch.smoca.redux.Store

class AppStore(initialState: AppState = AppState()) : Store<AppState>(initialState) {
    companion object {
        fun setup(): AppStore =
            AppStore().apply {
                addSaga(NetworkSaga(this::dispatch))
                addReducer(PersonReducer())
            }
    }
}
```





