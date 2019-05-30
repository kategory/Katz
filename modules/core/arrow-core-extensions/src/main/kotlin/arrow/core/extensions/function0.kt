package arrow.core.extensions

import arrow.Kind
import arrow.core.Function0
import arrow.core.ForFunction0
import arrow.core.Function0Of
import arrow.core.extensions.function0.monad.monad
import arrow.core.select as fun0Select
import arrow.core.Either
import arrow.extension
import arrow.core.fix
import arrow.core.invoke
import arrow.core.k
import arrow.typeclasses.Applicative
import arrow.typeclasses.Apply
import arrow.typeclasses.Bimonad
import arrow.typeclasses.BindingStrategy
import arrow.typeclasses.Comonad
import arrow.typeclasses.Functor
import arrow.typeclasses.Monad
import arrow.typeclasses.MonadContext
import arrow.typeclasses.MonadContinuation
import arrow.typeclasses.Monoid
import arrow.typeclasses.MonadFx
import arrow.typeclasses.Selective
import arrow.typeclasses.Semigroup

@extension
interface Function0Semigroup<A> : Semigroup<Function0<A>> {
  fun SA(): Semigroup<A>

  override fun Function0<A>.combine(b: Function0<A>): Function0<A> =
    { SA().run { invoke().combine(b.invoke()) } }.k()
}

@extension
interface Function0Monoid<A> : Monoid<Function0<A>>, Function0Semigroup<A> {
  fun MA(): Monoid<A>

  override fun SA() = MA()

  override fun empty(): Function0<A> =
    { MA().run { empty() } }.k()
}

@extension
interface Function0Functor : Functor<ForFunction0> {
  override fun <A, B> Function0Of<A>.map(f: (A) -> B): Function0<B> =
    fix().map(f)
}

@extension
interface Function0Apply : Apply<ForFunction0> {
  override fun <A, B> Function0Of<A>.ap(ff: Function0Of<(A) -> B>): Function0<B> =
    fix().ap(ff)

  override fun <A, B> Function0Of<A>.map(f: (A) -> B): Function0<B> =
    fix().map(f)
}

@extension
interface Function0Applicative : Applicative<ForFunction0> {
  override fun <A, B> Function0Of<A>.ap(ff: Function0Of<(A) -> B>): Function0<B> =
    fix().ap(ff)

  override fun <A, B> Function0Of<A>.map(f: (A) -> B): Function0<B> =
    fix().map(f)

  override fun <A> just(a: A): Function0<A> =
    Function0.just(a)
}

@extension
interface Function0Selective : Selective<ForFunction0>, Function0Applicative {
  override fun <A, B> Function0Of<Either<A, B>>.select(f: Kind<ForFunction0, (A) -> B>): Kind<ForFunction0, B> =
    fix().fun0Select(f)
}

@extension
interface Function0Monad : Monad<ForFunction0> {
  override fun <A, B> Function0Of<A>.ap(ff: Function0Of<(A) -> B>): Function0<B> =
    fix().ap(ff)

  override fun <A, B> Function0Of<A>.flatMap(f: (A) -> Function0Of<B>): Function0<B> =
    fix().flatMap(f)

  override fun <A, B> tailRecM(a: A, f: kotlin.Function1<A, Function0Of<Either<A, B>>>): Function0<B> =
    Function0.tailRecM(a, f)

  override fun <A, B> Function0Of<A>.map(f: (A) -> B): Function0<B> =
    fix().map(f)

  override fun <A> just(a: A): Function0<A> =
    Function0.just(a)

  override fun <A, B> Function0Of<Either<A, B>>.select(f: Kind<ForFunction0, (A) -> B>): Kind<ForFunction0, B> =
    fix().fun0Select(f)

  override fun <A> MonadContinuation<ForFunction0, *>.bindStrategy(fa: Function0Of<A>): BindingStrategy<ForFunction0, A> =
    BindingStrategy.Strict(fa.fix()())

  override val fx: MonadFx<ForFunction0>
    get() = Function0MonadFx
}

internal object Function0MonadFx : MonadFx<ForFunction0> {
  override val M: Monad<ForFunction0> = Function0.monad()
  override fun <A> monad(c: suspend MonadContext<ForFunction0>.() -> A): Function0<A> =
    Function0 { super.monad(c)() }
}

@extension
interface Function0Comonad : Comonad<ForFunction0> {
  override fun <A, B> Function0Of<A>.coflatMap(f: (Function0Of<A>) -> B): Function0<B> =
    fix().coflatMap(f)

  override fun <A> Function0Of<A>.extract(): A =
    fix().extract()

  override fun <A, B> Function0Of<A>.map(f: (A) -> B): Function0<B> =
    fix().map(f)
}

@extension
interface Function0Bimonad : Bimonad<ForFunction0> {
  override fun <A, B> Function0Of<A>.ap(ff: Function0Of<(A) -> B>): Function0<B> =
    fix().ap(ff)

  override fun <A, B> Function0Of<A>.flatMap(f: (A) -> Function0Of<B>): Function0<B> =
    fix().flatMap(f)

  override fun <A, B> tailRecM(a: A, f: kotlin.Function1<A, Function0Of<Either<A, B>>>): Function0<B> =
    Function0.tailRecM(a, f)

  override fun <A, B> Function0Of<A>.map(f: (A) -> B): Function0<B> =
    fix().map(f)

  override fun <A> just(a: A): Function0<A> =
    Function0.just(a)

  override fun <A, B> Function0Of<A>.coflatMap(f: (Function0Of<A>) -> B): Function0<B> =
    fix().coflatMap(f)

  override fun <A> Function0Of<A>.extract(): A =
    fix().extract()
}

fun <B> Function0.Companion.fx(c: suspend MonadContext<ForFunction0>.() -> B): Function0<B> =
  Function0.monad().fx.monad(c).fix()
