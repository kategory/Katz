@file:Suppress("DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE")

package com.pacoworks.typeclasses.basics.solved

import arrow.Kind
import arrow.fx.IO
import arrow.fx.IOPartialOf
import arrow.fx.extensions.io.async.async
import arrow.fx.typeclasses.Async
import arrow.fx.unsafeRunSync
import arrow.typeclasses.DaoDatabase
import arrow.typeclasses.Index
import arrow.typeclasses.NetworkModule
import arrow.typeclasses.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.CoroutineContext

fun <F> RequestOperationsAsync<F>.fetchUser(idx: Index): Kind<F, User> =
  idx.fetchUser()

class MyViewModel<F>(dep: RequestOperationsAsync<F>) : RequestOperationsAsync<F> by dep {
  fun onStart() {
    1.fetchUser()
  }
}

class MyActivity {
  fun onStart() {
    dependenciesAsValues.run { 1.fetchUser() }.unsafeRunSync()

    runBlocking { dependenciesAsValues.fetchUser(1) }

    runBlocking { MyViewModel(dependenciesAsValues).fetchUser(1) }
  }
}

val dependenciesAsValues: RequestOperationsAsync<IOPartialOf<Nothing>> =
  object : RequestOperationsAsync<IOPartialOf<Nothing>>,
    Async<IOPartialOf<Nothing>> by IO.async() {
    override val network: NetworkModule = NetworkModule()
    override val dao: DaoDatabase = DaoDatabase()
    override val ctx: CoroutineContext = Dispatchers.Default
  }

// Interlude: Retrofit, Dependency Injection and KEEP 87
