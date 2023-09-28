# Changelog

## 4.0.0 (TAG)
- Removed Action Generics. Actions should be placed as seald class in Reducer/Saga
- Changed Store class to abstract class. Reason: In Jetpack Compose, store should be makred with @Stable or unnecessary recompose will happen because the store will be makred as unstable.

## 3.0.1 (TAG)
- Fixed generic bugs

## 3.0.0 (TAG WITH MAJOR CHANGES)
- Added use of generic `Actions` for Sagas and Reducers
- Removed `Action` interface
    * Actions should now be implemented as sealed classes

## 2.0.2 (TAG)
### Same as 2.0.1

## 2.0.1 (TAG)
### Added the features of 1.0.5

## 2.0.0 (Branch)
### Head of 1.0.3 with the Following changes applied:
- Switched to plugins syntax

## 1.0.5 (TAG)
- Deprecated `plusAssign` of Sagas and Reducers to the Store and replaced them with `plus`
    * Sagas and Reducers are now added like this:
```kotlin
val store = Store(State())
store +
    Saga() +
    Reducer()
```

## 1.0.4 (TAG)
- Removed `kotlin-stdlib` gradle dependency

## 1.0.3 (BRANCH WITH MAJOR CHANGES)
- Removed `ReduxFragment`
- Removed `UI` (implement the fragment per project as it is not the job of a library)
- Removed `IOC` (we moved this to [another library](https://gitlab.smoca.ch/smoca/libraries/android-toolbox/ioc))
- Cleaned up code
- Added Javadocs

## (1.0.2.1) (COMMIT 2feef35e0abdb5c6a63e3df5c85d19daa99cad1a WITH MAJOR CHANGES)
### Between 1.0.2 and 1.0.3 ReduxFragment was added and removed. Some projects depend on this commit so you never have to search it ever again :)
- Added `ReduxFragment` () -> use this head if your project has redux.ui dependencies
- Added use of a generic `State` for the Store
- Replaced `LiveData` with `StateFlows`
- Added oldState, newState to `Saga::onAction`
- Added minSDK 16
- Cleaned up code
- Fixed a typo in Error message `presend` -> `present`

## 1.0.2 (TAG)
- Added comments to source code

## 1.0.1 (TAG)
- Formatted Android project

## 1.0.0 (TAG)
- Initial Version containing the a Store, Saga, Reducer, State and Action
