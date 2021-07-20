# Module main
# Arrow Fx Coroutines

[![Arrow Fx logo](https://raw.githubusercontent.com/arrow-kt/arrow-site/main/docs/img/fx/arrow-fx-brand-sidebar.svg?sanitize=true)](https://arrow-kt.io)

Λrrow Fx Coroutines is part of Arrow Fx [**Λrrow**](https://arrow-kt.io/docs/fx/), and the public documentation and information on how to use the library can be found there.

# Contributing and understanding the internals of Arrow Fx Coroutines

To understand how Arrow Fx Coroutines work, we need to take a step back and look at how Coroutines work in Kotlin.

We have several core types that are exposed from Kotlin Std, and the code rewrite performed by the compiler.

It's not very important to understand how the code rewrite works, the most important thing to understand is that it rewrites your `suspend` code into a very efficient state machine that allows flattening callbacks. In other words, when a method `suspends` it means that it will not return immediately but at a later point in time through a callback. This is important to know, since this concept also leaks into public API of coroutines.

## Kotlin Std Coroutines
### CoroutineContext

CoroutineContext is a immutable key-value map, where the keys are the types and the values are the instances of the type. Meaning that for every type, there can only be a single instance entry.
This is useful to for example ensure there is only a single `ContinuationInterceptor`, or a single `Job` in the case of KotlinX Coroutines or a single `SuspendConnection` in the case of Arrow Fx Coroutines.

A `suspend` function/program **always** has a `CoroutineContext`, this restriction is made in the `Continuation` interface.

This type-instance relationship is achieved by two interfaces `CoroutineContext.Element` and `CoroutineContext.Key<E: CoroutineContext.Element>`. Where `CoroutineContext.Element` has a `Key<*>`, which needs to point to a `Key` for it's concrete type. 

A simple example:

```kotlin
class CoroutineName(val name: String): CoroutineContext.Element {
  companion object Key : CoroutineContext.Key<CoroutineName>
  override val key: CoroutineContext.Key<*> = Key
}
```

### Continuation

```kotlin
interface Continuation<A> {
	val ctx: CoroutineContext
	fun resume(result: Result<A>)
}
```

As mentioned above, a `suspend` programs can return immediately or `suspend` and return it's value at a later point in time. Thus this can be modeled with a `Contination<A>` where both the immediate result or a suspended result is always returned to `resume`, this ensures that the result is always consumed in the same manner no matter how the `suspend` program behaves.

Important to note here is that the `resume` callback works with `Result<A>` which is similar to `Either<Throwable, Int>` but with a fast inlined implementation. This allows us to safely run any code inside `suspend`, whilst keeping our programs from blowing up since it will be safely rewired to `resume`.

Additionally it **always** has a `CoroutineContext`, which could be `EmptyCoroutineContext` (~ emptyMap()), or any other `CoroutineContext`.

Such a `Continuation` is always required to consume any `suspend` program through the runners exposed by the Kotlin Standard Library.

### startCoroutine

The most high level runner in the standard library is `startCoroutine`, and it's the first method we'll discuss that creates a `Coroutine` and runs it. Kotlin’s standard library defines a Coroutine as an instance of a suspendable computation. In other words, a Coroutine is a compiled `suspend () -> T` program wired to a Continuation.

Let’s take a quick look at an example with the 3 types we've seen and discussed above.

```kotlin
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

suspend fun one(): Int = 1

suspend fun fail(): Int = TODO()

fun main() {
  ::one.startCoroutine(Continuation(EmptyCoroutineContext) {
    println(it) // Success(1)
  })

  ::fail.startCoroutine(Continuation(EmptyCoroutineContext) {
    println(it) // Failure(kotlin.NotImplementedError: An operation is not implemented.)
  })
}
```

Here you can see that even if our program blows up, it will be correctly wired into the `Continuation` callback without blowing up `main`.

If we take a look at its very short implementation, we can see that it exists of a composition of 3 functions. `createCoroutineUnintercepted` with the completion `Continuation`, `intercepted` and `resume`. In the sections below we'll dig into what that means, and how that works.
All you really need to remember here is that `suspend () -> T` will run on a given `Continuation<T>`, on its `CoroutineContext`. If that `CoroutineContext` is capable of interception, it will intercept first and then it will start with `resume(Unit)`.

```
public fun <T> (suspend () -> T).startCoroutine(completion: Continuation<T>) {
    createCoroutineUnintercepted(completion).intercepted().resume(Unit)
}
```

### ContinuationInterceptor

Before we can move into what `createCoroutineUnintercepted` is or how it works, we should first understand what interception or `intercepted` is in Kotlin Coroutines.
One thing that is important to understand is that Coroutines by themselves have nothing to do with concurreny or asynchronity, but rather they can be used to build concurrent frameworks or libraries.

So let's take a quick look at its definition

```kotlin
public interface ContinuationInterceptor : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<ContinuationInterceptor>

    public fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T>

    public fun releaseInterceptedContinuation(continuation: Continuation<*>) {
        /* do nothing by default */
    }
}
```

Here we can see that `ContinuationInterceptor` is a `CoroutineContext.Element`, which means we can put into an immutable `CoroutineContext` map. It has an abstract `interceptContinuation` method, which is called upon calling `intercepted`. It can be called upon the `Continuation` returned by `createCoroutineUnintercepted`, and in that case will call `interceptContinuation` in the case the `CoroutineContext` contains a `ContinuationInterceptor`. This operation is idempotent, since the result is cached.

When the `Continuation` is finished running, then before returning it will call `releaseInterceptedContinuation`, but only in the case `interceptContinuation` returned a different `Continuation` than it received.

Let's take a look at a simple example.

```kotlin
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine

suspend fun helloWorld(): String = "Hello World!"

fun main() {
  ::helloWorld.startCoroutine(Continuation(Printer) {
    println("Received result: $it")
  })
}

object Printer : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
  override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> {
    val newContinuation = object : Continuation<T> {
      override val context: CoroutineContext = continuation.context

      override fun resumeWith(result: Result<T>) {
        println("PrinterContinuation resume: pass $result to original continuation")
        continuation.resumeWith(result)
      }
    }
    println("interceptContinuation($continuation)\nand returning a new one: $newContinuation\n")
    return newContinuation
  }

  override fun releaseInterceptedContinuation(continuation: Continuation<*>) {
    super.releaseInterceptedContinuation(continuation)
    println("releaseInterceptedContinuation($continuation)")
  }
}
```

```
1. interceptContinuation(Continuation at kotlin.coroutines.intrinsics.IntrinsicsKt__IntrinsicsJvmKt$createCoroutineUnintercepted$$inlined$createCoroutineFromSuspendFunction$IntrinsicsKt__IntrinsicsJvmKt$2) 
and returning a new one: Printer$interceptContinuation$newContinuation$1@448139f0

2. PrinterContinuation resume: pass Success(kotlin.Unit) to original continuation

3. releaseInterceptedContinuation(Printer$interceptContinuation$newContinuation$1@448139f0)

4. Received result: Success(Hello World!)
```

Let's analyse the output:

In the first line of the output we can clearly see that our original `Continuation` created by `startCoroutine` is one created by the Kotlin Compiler through `createCoroutineUnintercepted` (and some other internal machinery inside the Kotlin Std library which creates a `Coroutine` from a suspending function). We wrap it, and return our own `Printer` continuation.

Secondly, our `Printer` continuation is started with `resume(Unit)`, which we see reflected as receiving `Success(kotlin.Unit)` in `resume` and then simply passing it along to our original continuation. The original continuation, created by the Kotlin Compiler, will then start our `helloWorld` suspend program.

3. After our program is finished, we see that our `Printer` continuation is released.

4. Finally, we see that our result is finally returned to our own `Continuation`.

So you might be wondering why this is useful? This isn't very useful for printing anything obviously, because we only get `intercepted` before anything starts running and released when it's finished. But it gives us the perfect entry point to schedule the coroutine on a `Thread` or `ExecutorService`.

#### Intercepting to schedule/dispatch

Scheduling something on a `ExecutorService` is simply a call to the function `ExecutorService#submit` with a `Runnable` argument. Which is exactly what we can do with interception, looking at the previous log output, we can intercept the _start_ signal of `Result<Unit>` before we pass it to the original continuation. This means we can intercept the _start_ signal, and `submit` it on an `ExecutorService` before starting the original continuation.
So let's see the most basic example where we do this for the `ForkJoinPool`.

```kotlin
object CommonPoolContext : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
  val pool: ForkJoinPool = ForkJoinPool()

  override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
    object : Continuation<T> {
      override val context: CoroutineContext = continuation.context

      override fun resumeWith(result: Result<T>) {
        pool.submit { continuation.resumeWith(result) }
      }
    }
}
```

We create a singleton `ContinuationInterceptor` just like we did for our `Printer`, and we when we get intercepted we can then wrap the original `Continuation` with a `Continuation` that `submit`s the _start_ signal onto the `ForkJoinPool`.

A small example to see this in action, we need the `Thread.sleep` in `main` since when we reach `pool.submit` in the `Continuation` `main` will finish and we want to wait a bit so we can see the ouput printed.

```kotlin
import java.util.concurrent.ForkJoinPool
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.startCoroutine

suspend fun helloWorld(): String =
  "Hello from ${Thread.currentThread().name}"

object CommonPoolInterceptor : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
  val pool: ForkJoinPool = ForkJoinPool()

  override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
    object : Continuation<T> {
      override val context: CoroutineContext = continuation.context

      override fun resumeWith(result: Result<T>) {
        pool.execute { continuation.resumeWith(result) }
      }
    }
}

fun main() {
  ::helloWorld.startCoroutine(Continuation(CommonPoolInterceptor) {
    println(it)
  })

  Thread.sleep(100)
}
```

Now that we know what it means to `intercept` and why it's crucial to build effect systems on top of Kotlin Coroutines, let's investigate some more of the lower level combinators. Those will allow us to by-pass `intercepted` from the higher level combinators to avoid scheduling all over the place, or allowing us to change `CoroutineContext` without having to explicitly schedule. This is important in relation to cancellation, which we'll see in great detail below.

### createCoroutineUnintercepted

`createCoroutineUnintercepted` is the lowest level function of creating (and thus subsequently starting) a Coroutine, and you'll find it is used in most other function with either `createCoroutineXXX` or `startCoroutineXXX`.

It's found in the `kotlin.coroutines.intrinsics` package, which we saw 1 method of so far `intercepted`. And we'll cover the other content below. As we've already mentioned above, this method turns a `suspend () -> T` function into a `Coroutine`, which is an instance of a suspendable computation. This Coroutine hasn't started running yet, and we can run it as many times as we want. As you might've guessed from above, we can start the Coroutine by calling `resume(Unit)` on it.

For a given program `suspend () -> T` we pass a `Continuation<A>`, and `createCoroutineUnintercepted` returns us a `Continuation<Unit>`. This `Continuation<Unit>` models a Coroutine, which is the program `suspend () -> T` wired to it's `context` and callback `resume: (Result<T>) -> Unit`.

Let's see a quick example of the creation of a simple Coroutine which is started multiple times.

```kotlin
import kotlin.coroutines.intrinsics.createCoroutineUnintercepted
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume

suspend fun one(): String = "Hello World!"

val cont: Continuation<Unit> = ::one
  .createCoroutineUnintercepted(Continuation(EmptyCoroutineContext, ::println))

fun main() {
  cont.resume(Unit)
  println("Fired once")

  cont.resume(Unit)
  println("Fired Twice")

  cont.resume(Unit)
  println("Fired three times")
}
```

If we look back at `startCoroutine`, we see that is simply creates a Coroutine, **always** invokes its interceptor and then immediately starts it. So the highest level runner, combines the lowest level functions. 

```
public fun <T> (suspend () -> T).startCoroutine(completion: Continuation<T>) {
    createCoroutineUnintercepted(completion).intercepted().resume(Unit)
}
```

### suspendCoroutineUninterceptedOrReturn / COROUTINE_SUSPENDED

As you might've noticed in all examples above, we haven't actually used any suspension so far. But what does it mean to suspend? Most simply put it means the function doesn't immediately returns, but is going to return the result through the callback `(Result<T>) -> Unit`.

At the lowest guts of writing suspend functions you can find this function, is capable of returning immediately or returning in a suspended way. We however have to signal to Kotlin's Coroutine system that we're returning immediately or we're suspending and returning at a later point in time. This is done with the marker `COROUTINE_SUSPENDED`.

`COROUTINE_SUSPENDED` is a simply marker that can be compared to check if it's the users result, or we should wait for `Continuation#resume` to be called. In the Kotlin Standard Library we can find following definition.

```
val COROUTINE_SUSPENDED: Any
```

So in combination with `suspend inline fun <T> suspendCoroutineUninterceptedOrReturn(crossinline block: (Continuation<T>) -> Any?): T`, we can now write `suspend fun` which suspend or return immediately.

```kotlin
suspend fun immediateHelloWorld(): String =
  suspendCoroutineUninterceptedOrReturn { cont ->
    "Hello World!"
  }

suspend fun suspendingHelloWorld(): String =
  suspendCoroutineUninterceptedOrReturn { cont ->
    cont.resumeWith(Result.success("Hello World!"))
    COROUTINE_SUSPENDED
  }
```

Here you can see that in `immediateHelloWorld` we immediately return `"Hello World!"` to the `suspendCoroutineUninterceptedOrReturn` function, and in `suspendingHelloWorld` we return the value through the `Continuation#resume` callback and signal to the Coroutine that we returned the result through the callback.

This is useful when you need to access the `CoroutineContext`, but you know you don't actually need to `suspend` since it allows accesing the `Continuation` and its `CoroutineContext` without having to suspend.
Or when you want to suspend the coroutine but you don't want to explicitely `intercept` again, this is a useful optimisation when for example the `CoroutineContext` contains a `ContinuationInterceptor` that dispatches/schedules which is an expensive operation when it's  unnecessary.

#### suspendCoroutine

So what is the difference with the higher level and better known `suspendCoroutine` function? As the name of the lower level function might have hinted, this function always calls `intercepted` but additionally it also removes the ability to immediately return **whilst still allowing for this optimisation underneath at a relative small cost!**.

The implementation of `suspendCoroutine` looks like this:

```
@SinceKotlin("1.3")
@InlineOnly
public suspend inline fun <T> suspendCoroutine(crossinline block: (Continuation<T>) -> Unit): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return suspendCoroutineUninterceptedOrReturn { c: Continuation<T> ->
        val safe = SafeContinuation(c.intercepted())
        block(safe)
        safe.getOrThrow()
    }
}
```

As you can see here it's build on top of the lower level function `suspendCoroutineUninterceptedOrReturn`, which we've discussed above, but it calls `intercepted` on the `Continuation`. This is done to add some additional safety, **which only works in the case the `CoroutineContext` contains a dispatching `ContinuationInterceptor`**.

Let's say that inside `suspendCoroutine` you wrap some Java API that runs on an unbounded cached pool (commonly known as `IO` pools) to run blocking IO operations such as JDBC, it would be dangerous to remain on this pool since we could unknownlingly exhaust the JDBC pool deadlocking calls to JDBC. For this reason it's much safer to always call `intercepted` so that after suspension we return to the original context. Similarly to how `evalOn` or `withContext` return to their original context after running an operation on a different pool. This is a safety measure commonly used in effect systems in other languages as well such as `IO.async` from Cats Effects in Scala. Important to note is that this only works if a dispatching `ContinuationInterceptor` is available in the `CoroutineContext`.

So what is `SafeContinuation`? `SafeContinuation` runs the code provide by the `block` lambda, and in case that it calls `resumeWith` in an immediate manner we want to return that result immmediately to `suspendCoroutineUninterceptedOrReturn`. In the case it for example forks to a different context, for example for JDBC or a network call, we cannot return immediately and we need to return `COROUTINE_SUSPENDED` such as explained above. This machinery and state machine is hidden inside `SafeContinuation`. Simply put, `getOrThrow` is raced with `resumeWith` to see which one is reached first and depending on who wins it returns the result or `COROUTINE_SUSPENDED`. You can check the implementation [here](https://github.com/JetBrains/kotlin/blob/master/libraries/stdlib/jvm/src/kotlin/coroutines/SafeContinuationJvm.kt).

### startCoroutineUninterceptedOrReturn

The last method we haven't discussed yet is `startCoroutineUninterceptedOrReturn`, which is similar to `startCoroutine` but as you might've guessed doesn't call `intercepted`. Additionally, it's more raw bones since it either returns us the result immediately or it returns us the result in the `Continuation` callback.

If we look at the implementation, we can also clearly see how it works in a desugared way on the JVM.

```kotlin
@SinceKotlin("1.3")
@InlineOnly
public actual inline fun <T> (suspend () -> T).startCoroutineUninterceptedOrReturn(
    completion: Continuation<T>
): Any? = (this as Function1<Continuation<T>, Any?>).invoke(completion)
```

The `suspend () -> T` function is casted to a `Function1<Continuation<T>, Any?>`, and we simply pass the `completion` callback as an argument. If you think back to `suspendCoroutineUninterceptedOrReturn` which either passed the result to the callback or returned immediately it's clear how these two could be wired together.
So let's take back our examples from `suspendCoroutineUninterceptedOrReturn` and see how they behave when we run them with `startCoroutineUninterceptedOrReturn`.

```kotlin
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

suspend fun immediateHelloWorld(): String =
  suspendCoroutineUninterceptedOrReturn { cont ->
    "Hello World!"
  }

suspend fun suspendingHelloWorld(): String =
  suspendCoroutineUninterceptedOrReturn { cont ->
    cont.resumeWith(Result.success("Hello World!"))
    COROUTINE_SUSPENDED
  }

fun main() {
  ::immediateHelloWorld.startCoroutineUninterceptedOrReturn(Continuation(EmptyCoroutineContext) {
    println("I am never called")
  }).also { println("Immediately returned: $it") }

  ::suspendingHelloWorld.startCoroutineUninterceptedOrReturn(Continuation(EmptyCoroutineContext) {
    println("I am called with result: $it")
  }).also { println("suspending returned: $it") }
}
```

Output:
```
Immediately returned: Hello World!
I am called with result: Success(Hello World!)
suspending returned: COROUTINE_SUSPENDED
```

Here in the output we can clearly see that in the first case the callback is never invoked, and we get the result immediately returned to us.
In contrast to the second case where the callback is invoked, **and** the immediate returned value is `COROUTINE_SUSPENDED`.
This means that if we use `startCoroutineUninterceptedOrReturn` you **always** need to check the returned value to check if it's `COROUTINE_SUSPENDED` or a user returned value (which in that case needs to be casted back from Any? to T).

This might all seem very obscure, but very powerful as we'll discuss below how we use these primitives to implement some of Arrow Fx Coroutines core operations.

## Arrow Fx Coroutines

When building an effect system it's crucial that we take into account cancellation, since in todays high traffic back-ends or fast paced web/mobile users its important we can cancel request when a user or device is not interested in receiving the result anymore. 

### SuspendConnection

At the basis of this cancellation is a `sealed class SuspendConnection`, which is also a `CoroutineContext` and is defined as follows:

```kotlin
sealed class SuspendConnection : AbstractCoroutineContextElement(SuspendConnection) {
  companion object Key : CoroutineContext.Key<SuspendConnection>
  
  abstract suspend fun cancel(): Unit
  abstract fun isCancelled(): Boolean

  abstract fun push(token: CancelToken): Unit

  // ...
}
```

A `CancelToken` is a `inline class` wrapper around `suspend () -> Unit` which is used to model cancellation token. These are used to define cancellable operations in a typed manner.

It has two implementations, `Uncancellable` and `DefaultConnection`

#### Uncancellable

The uncancellable implementation has a no-op `cancel` method, and always returns `false` upon checking `isCancelled`.
Which subsequently means that all cancellable operations become uncancellable when run on this `CoroutineContext`.
Some optimisations could be implemented when an `uncancellable` context is detected.

#### DefaultConnection

The `DefaultConnection` keeps track of all `CancelToken` registered for a given `SuspendConnection`.
It does so by keeping all `CancelToken` in a FIFO stack, and running them in order when cancelled.
Since `SuspendConnection` is a FIFO stack, we can register a token before running a cancellable operation,
and pop the token after the cancellable operation has finished so
we don't run a `CancelToken` when it's not necessary anymore since that could lead to (undefined) weird behavior. This is useful for example in `parMap2` where we want to launch two parallel operations, and push their cancellation system onto to the original `SuspendConnection` and `pop` it when both finish.

### Cancellation

So what does it actually mean to cancel? Currently in Arrow 0.11.0, cancellation means that the code stops running (or in other words suspends forever and `never` returns).
This is implemented as a `cancelBoundary`, and with all the understanding of the low-level Kotlin machinery we have now can be implemented as follows.

```kotlin
suspend fun cancelBoundary(): Unit =
  suspendCoroutineUninterceptedOrReturn { cont ->
    if (cont.context[SuspendConnection]?.isCancelled() == true) COROUTINE_SUSPENDED
    else Unit
  }
```

So a `cancelBoundary` checks if the `SuspendConnection#isCancelled` and if so it suspends forever and never invokes the callback, effectively this means it stops running the code.
In the case the `SuspendConnection` isn't cancelled, it immediately returns `Unit`. Here we use `suspendCoroutineUninterceptedOrReturn` since we can return immediately when no cancellation signal was triggered, and we don't want to intercept or schedule but we have to check the `Continuation#context` to be able to access the `SuspendConnection`.

So this is how from any user or library code we can add checks for cancellation that automatically exit when a cancellation signal was trigerred.

When `isCancelled` returns `true`, this means that all the registered `CancelToken`'s inside `SuspendConnection` have run, or are running.

NOTE:
In Kotlin 1.4, CancellationException was added to the Kotlin Std as an ExperimentalApi. The documentation states that a cancellable coroutine should throw it to signal that it has been canccelled. In a future version of Arrow Fx Coroutines we can follow this pattern by instead of suspend upon cancellation like we did in Arrow Fx IO, and as is known from Cats Effects IO, we can `throw CancellationException`. This would also only happen when we're running on a cancellable `SuspendConnection`, and we'd still be able to control cancellation in the same way as you might be familiar with from Cats Effects IO or Arrow Fx IO.

### startCoroutineCancellable

So how do we start a Coroutine with `SuspendConnection`, so we can start `suspend () -> T` in a cancellable way. Using `SuspendConnection` is no different than using any other `CoroutineContext`, so let's take a look at the most simple example we can come up with.

```kotlin
import arrow.fx.coroutines.CancelToken
import arrow.fx.coroutines.SuspendConnection
import arrow.fx.coroutines.seconds
import arrow.fx.coroutines.sleep
import kotlin.coroutines.Continuation
import kotlin.coroutines.startCoroutine

suspend fun main() {
  val connection = SuspendConnection()
  connection.push(CancelToken { println("CancelToken ran") })

  suspend { sleep(1.seconds) }.startCoroutine(Continuation(connection) {
    println("This never runs, since sleep was cancelled.")
  })

  connection.cancel()

  Thread.sleep(2000) // Sleep longer than suspend program
}
```

Output
```
CancelToken ran
```

As we can see in the output, it only prints the `println` from the `CancelToken`. The reason that we also don't see the output from the `Continuation` callback is because it was cancelled before reaching it, since the `sleep` operation gets cancelled and thus never resumed.

So one of the final questions is, how do we build cancellable operators such as `sleep`.

### cancellable (suspendCoroutineCancellable)

To understand how `cancellable` operations are build, let's take a quick look at the implementation of `sleep`. If you're familiar with Arrow's IO, or another `IO` implementation the code below might look familiar.

```kotlin
internal val scheduler: ScheduledExecutorService = TODO()

suspend fun sleep(duration: Duration): Unit =
  if (duration.amount <= 0) Unit
  else cancellable { resumeWith: (Result<Unit>) -> Unit ->
    val cancelRef = scheduler.schedule(
      { resumeWith(Result.success(Unit)) },
      duration.amount,
      duration.timeUnit
    )

    CancelToken { cancelRef.cancel(false) }
  }
```

We open a `cancellable` block and get a callback which we can invoke with either a pure value, or a `Throwable` failure. We can then schedule a task to resume the callback after the desired `Duration` has passed, and to the `cancellable` block we return the `CancelToken` that we want to execute if the resulting `suspend fun` gets cancelled.

So the signature of `cancellable` looks like:

`suspend fun <A> cancellable(cb: ((Result<A>) -> Unit) -> CancelToken): A`

Where we create a `cancellable` `suspend fun` by consuming a callback of the shape `((Result<A>) -> Unit)` and returning `CancelToken`. 

So let's take a look at what the implementation of `cancellable` looks like.

```
suspend fun <A> cancellable(cb: ((Result<A>) -> Unit) -> CancelToken): A =
  suspendCoroutine { cont ->
    val conn = cont.context[SuspendConnection] ?: SuspendConnection.Uncancellable

    val cancellable = ForwardCancellable()
    conn.push(cancellable.cancel())

    val token = try {
      cb(cont::resumeWith)
    } catch (throwable: Throwable) {
      cont.resumeWith(Result.failure(throwable.nonFatalOrThrow()))
      CancelToken.unit
    }
    
    cancellable.complete(token)
  }
```

We `suspendCoroutine` to access the `Continuation`, and since we use `suspendCoroutine` here it will also `intercept` and thus behave in the same safe way as Kotlin's high-level combinators.
We retrieve the `SuspendConnection`, and default back to an `Uncancellable` connection.

Since we don't know how long it will take before `cb: ((Result<A>) -> Unit) -> CancelToken` returns the `CancelToken` we need a mechanism to back-pressure `conn`, when it gets cancelled, until we receive the `CancelToken`. Otherwise `conn` might get cancelled whilst `CancelToken` is not registered yet creating a race-condition and possibly leaking resources.

`ForwardCancellable` allows us to _defer_ or back-pressure the cancellation signal from the original `conn`, until we receive the `complete` signal with the resulting `CancelToken`.

So here we create a `ForwardCancellable` and push its `cancel` token onto the `SuspendConnection`. We run the _foreign_ user code with the `resumeWith` callback, and `complete` the `ForwardCancellable` with the returned `CancelToken`.
In case of an exception we `resumeWith` the exception and return a no-op `CancelToken`.

#### ForwardCancellable

ForwardCancellable is useful to forward cancellation when you want to inject an uncancellable piece of logic,
or defer cancellation and wire two different `SuspendConnection`'s together.

ForwardCancellable can be cancelled and completed with a `CancelToken`.

The following 4 scenarios can occur:

 - When `cancel` is called before `complete`, then `cancel`'s token is back-pressured until `complete` is called.
   Then `cancel`'s token will subsequently call the CancelToken that was passed to `complete`.

 - When `cancel` is called after `complete`, then `cancel` immediately runs the cancellation token passed to `complete`.

 - When the `ForwardCancellable` is completed, before `cancel`'s token is invoked.
 The completed `CancelToken` will be invoked immediately when `cancel`'s token is invoked.

 - Calling `complete` twice will result in an `ArrowInternalException`, and is considered an internal developer bug.
