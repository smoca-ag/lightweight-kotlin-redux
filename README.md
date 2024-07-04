<html>
<div align="center">
  <a href="https://central.sonatype.com/artifact/ch.smoca.lib/lightweight-kotlin-redux" ><img src="https://img.shields.io/badge/mavenCentral-6.1.0-A1C83D?style=for-the-badge" alt="Version"></a>
  <a href="./LICENSE"><img src="https://img.shields.io/badge/License-MIT-A1C83D?style=for-the-badge" alt="License"></a>
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge" alt="Kotlin">

</div>
</html>

# How To Use

This library can be used for two scenarios which differ in creation:
- [Lightweigt Kotlin Redux](#lightweigt-kotlin-redux)
- [Project Setup](#project-setup)
  + [KMP State Definition](#kmp-state-definition)
  + [Native Implementation](#native-implementation)
    - [iOS](#ios)
    - [Android](#android) 
- [Tutorial](#tutorial)



# Lightweigt Kotlin Redux

The Lightweight Kotlin Redux Library is an opinionated implementation of the Redux Architecture.
It is used in many projects of the Smoca AG and has some design choices that fit our needs.
Following is a description of the components and how they are intended to be used.

## State

- The data
- Only use data classes with `val` properties. 
- The state MUST be immutable

The state decribes the data of the whole system. The state must not be mutable. Only data classes with read-properties are allowed. The only component that can change the state is the [Reducer](#reducer)

```kotlin
data class AppState(
    val count: Int = 0
) : State
```

## Store

- Holds the observable State
- Informs listeners of State changes
- Can be used to dispatch actions
- Passes Action and current State to Middlewares and Reducers
- Passes Action, dispatch function, Old State and New State to Sagas

```kotlin
//create a store
val store = Store<AppState>(
  initialState =  /*initial state if any*/,
  reducers = listOf(/*List of all reducers*/),
  sagas = listOf(/*List of all sagas*/),
	middlewares = listOf(/*List of all middlewares*/)
)
...
// listen to changes
store.stateObservable.collect() { state ->
	Log.d("Change", "State: $state")
}
//or if you use Jetpack Compose
val state by store.stateObservable.collectAsState()
//dispatch some action. The action will run on a different thread.
store.dispatch(Add(amount = 1))

```

All actions will be run on a single thread of the store. Dispatching an action will therefore never block the calling thread. Changes musst be observerd through the stateObservable.

## Reducer

- Listens to Actions
- Manipulates the State
- MUST NOT hold any state. 
- Will return a new state depending on the action

```kotlin
class CountReducer: Reducer<AppState> {
  //seald class lets compile check if 'when' expression is exhaustive
    sealed class CountAction : Action {
        data class Add(val amount: Int) : CountAction()
    }

    override fun reduce(action: Action, state: AppState): AppState {
        (action as? CountAction) ?: return state //only process actions that concern us, otherwise return state
        when (action) {
            is CountAction.Add -> {
              	//the copy-function on each data class can be used to create a new state
                return state.copy(count = state.count + action.amount)
            }
        }
    }
}
```

Reducers get all the dispatched actions (if not canceled or altered by a middleware). It is good practice to create a reducer per domain (e.g., Person, Network...) and only react to the actions that belong to the corresponding reducer.
Also, if sealed classes are used as actions, the compiler will be able to check if the 'when' expression is exhaustive.

Each reducer must return a state. If the state needs to be changed, kotlins `.copy()` function can be used, allowing you to alter some of its properties while keeping the rest unchanged. This way, a new state with new values can be returned without altering the original state.

## Middleware

- Listens to action
- Has access to the store 
- Can change or abort action flow
- Can have an internal state

Middlewares will be called before any reducer and can abort the action chain, alter the action and/or dispatch new actions.

The following Middleware simply logs the action, the old and the new state.

```kotlin
class LogMiddleware : Middleware<AppState> {
    override fun process(
        action: Action,
        store: Store<AppState>,
        next: (action: Action) -> Unit
    ) {
        val currentState = store.getState() //read the current state from the store
        // next(action) will pass the action to the next middleware in the chain. If next is not called, the action is aborted.
        val result = next(action) 
        // After the call to next, the action is reduced into the state (if no other middleware further down the road cancels it)
        val newState = store.getState()
        Log.d(
            this::class.simpleName,
            "Diff:\n" +
            "Action:\n$action\n" +
            "Old:\n$currentState\n" +
            "New:\n$newState"
        )
    }
}
```

In other Redux implementation, a middleware can return a value. This is not supported in this implementation, since `dispatch(action)` runs on a differtent thread and can not return anything. 

## StateObserver

- Listens to state changes
- Can hold internal state
- May run async methods

A `StateObserver` observes the state and may do some work if something in the state changes. This helps to truly encabluate the logic from the rest of the code. If more flexibility is needed (maybe trigger something by an action), [Sagas](#saga) may help.

To use the  `StateObserver` the  `StateObserverMiddleware` must be provided to the store.

```kotlin
class ExampleStateObserver: StateObserver<TestState>() {
   override fun onStateChanged(state: TestState) {
      /*state has changed. */
      if (state.testProperty == 1) {
      /* do something */
   		}
   }
 }
```

`onStateChanged` will be called on coroutine with limitedParallelism = 1. The methode may be called again as soon as the coroutine is freed (for example when calling a other suspending function).

The `StateObserver` will be called for any state change, not just the specific changes it accesses. It is the responsibility of the `StateObserver` to ensure that processes are not unintentionally triggered more than once.

## Saga

* Can have internal state
* Listens to Actions
* Has access to old and new state
* Can dispatch actions
* Will be called on its own coroutine view.
* Is intended for longrunning or asynchrone operation (calculations, fetch network data...)

Sagas are typically initiated by an action and then proceed through multiple processing steps. When used with `CancellableSagaMiddleware`, the steps can be canceled if necessary. With `QueueingSagaMiddleware`, the subsequent actions are queued until all steps of the preceding action are fully completed.
Each saga will be called on its own coroutine whit limitedParallelism = 1.

To use Sagas,  `CancellableSagaMiddleware` or `QueueingSagaMiddleware` must be provided to the store.

```kotlin
class BusySaga() : Saga<AppState>() {
    sealed class Work: Action {
        data object DoWork: Work()
     
    }
    override suspend fun onAction(action: Action, oldState: AppState, newState: AppState) {
        (action as? Work)?.let {
            when (it) {
                Work.DoWork -> {
                    // a lof of heavy lifting
                  	...
                  //the saga has access to the dispach-function and can dispatch new action that should be processed by a reducer
                  dispatch(WorkResult())
                }
            }
            
        }
    }
}
```

For convenience, the saga gets the state (old state) before the action and the state after the action (new state). To add some data to the state, it must dispatch an action.

If only certain actions can be processd by a Saga, overwrite `acceptAction` and return the sealed class that defines the action.



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

9. The Project should now sync and build successfully. Continue by [creating all necessary components](#lightweigt-kotlin-redux)

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

 

# Tutorial


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

class NetworkSaga() : Saga<AppState>() {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    var baseUrl: String = "https://api.person.lol/"

    sealed class NetworkAction : Action {
        data class FetchPerson(val endpoint: String = "1") : NetworkAction()
    }

    override suspend fun onAction(action: Action, oldState: AppState, newState: AppState) {
        val networkAction = action as? NetworkAction ?: return
        when (networkAction) {
            is NetworkAction.FetchPerson -> getPerson(networkAction)
        }
    }

    private suspend fun getPerson(action: NetworkAction.FetchCatFact) {
          val url = "$baseUrl${action.endpoint}"
          val response = client.get(url)
          if (response.status.value in 200..299) {
              val person: Person = response.body()
              dispatch(PersonReducer.PersonAction.UpdateName(person.name))
          }
    }
}
```

5. Reducers, Sagas and Middlewares are supplied to the store in the constructor. :
```kotlin
private fun setUpStore(): Store<AppState> {
        val store = Store<AppState>(
            initialState =  null,
            reducers = listOf(/List of all reducers/),
            sagas = listOf(/List of all sagas/),
            middlewares = listOf(/List of all middlewares/)
        )
        return store
    }
```

# License
 The MIT License (MIT)

Copyright © 2024 Smoca AG

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
