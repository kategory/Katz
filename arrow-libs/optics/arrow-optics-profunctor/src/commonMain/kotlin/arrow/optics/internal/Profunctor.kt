package arrow.optics.internal

import arrow.core.Either
import arrow.core.Eval
import arrow.typeclasses.Monoid

interface Kind<out F, out A>
interface Pro<P, in I, in A, out B>

// We are not using default impl anywhere because unlike haskell composition won't
//  ever fuse and produce equally performant implementations, also its not that much
//  of an inconvenience since we don't have too many types implementing this

/**
 * A profunctor is a bifunctor with the first argument being contravariant and the
 *  second argument covariant.
 *
 * The easiest profunctor to visualize is a function with one argument `(A) -> B`.
 * This function is contravariant in `A` and covariant in `B`.
 *
 * A profunctor allows us to either contramap `lMap` or map `rMap` either of its arguments,
 * or both at once using `dimap`
 *
 * For one argument functions `lMap == compose` `rMap == andThen/map`.
 *
 * This implementation of profunctors is tailored for optics and more specifically indexed optics.
 * The `ixMap` method exists solely to contramap over the index and servers no other purpose
 *  related to profunctos.
 */
interface Profunctor<P> {
  fun <I, A, B, C, D> Pro<P, I, B, C>.dimap(f: (A) -> B, g: (C) -> D): Pro<P, I, A, D>
  fun <I, A, B, C> Pro<P, I, B, C>.lMap(f: (A) -> B): Pro<P, I, A, C>
  fun <I, B, C, D> Pro<P, I, B, C>.rMap(g: (C) -> D): Pro<P, I, B, D>

  fun <I, J, A, B> Pro<P, J, A, B>.ixMap(f: (I) -> J): Pro<P, I, A, B>
}

/**
 * The strong typeclass extends profunctors with the ability to pass-through an argument.
 *
 * It essentially allows us to ignore an argument, in this case a part of a pair when
 *  running a profunctor. With just `dimap` we always have to consume the full argument.
 */
interface Strong<P> : Profunctor<P> {
  fun <I, A, B, C> Pro<P, I, A, B>.first(): Pro<P, I, Pair<A, C>, Pair<B, C>>
  fun <I, A, B, C> Pro<P, I, A, B>.second(): Pro<P, I, Pair<C, A>, Pair<C, B>>

  fun <I, J, S, T, A, B> Pro<P, J, A, B>.ilinear(f: IxLinearF<I, S, T, A, B>): Pro<P, (I) -> J, S, T>
}

interface IxLinearF<I, S, T, A, B> {
  operator fun <F> invoke(FF: Functor<F>, s: S, f: (I, A) -> Kind<F, B>): Kind<F, T>
}

/**
 * Co-strong is the dual of strong and is more relevant in lazy languages since it can be
 *  used to implement looping with just profunctors.
 *
 * For this package it is only important in terms of the `Tagged` profunctor which has
 *  a phantom type for the contravariant side and just holds a value we can easily
 *  map over. It is also used in the `Re` profunctor which reverses the direction of the
 *  underlying profunctor.
 */
interface CoStrong<P> : Profunctor<P> {
  fun <I, A, B, C> Pro<P, I, Pair<A, C>, Pair<B, C>>.unFirst(): Pro<P, I, A, B>
  fun <I, A, B, C> Pro<P, I, Pair<C, A>, Pair<C, B>>.unSecond(): Pro<P, I, A, B>
}

/**
 * Choice augments our profunctor with the ability to branch based on its input.
 */
interface Choice<P> : Profunctor<P> {
  fun <I, A, B, C> Pro<P, I, A, B>.left(): Pro<P, I, Either<A, C>, Either<B, C>>
  fun <I, A, B, C> Pro<P, I, A, B>.right(): Pro<P, I, Either<C, A>, Either<C, B>>
}

/**
 * Co-choice is the dual of choice and similarly only useful with the `Tagged` and `Re`
 *  profunctors.
 */
interface CoChoice<P> : Profunctor<P> {
  fun <I, A, B, C> Pro<P, I, Either<A, C>, Either<B, C>>.unLeft(): Pro<P, I, A, B>
  fun <I, A, B, C> Pro<P, I, Either<C, A>, Either<C, B>>.unRight(): Pro<P, I, A, B>
}

/**
 * Traversing extends profunctors with the ability to embed traversals (in VL style)
 *  into the profunctor structure. This is what allows `Traversal` to exist.
 */
interface Traversing<P> : Strong<P>, Choice<P> {
  fun <I, S, T, A, B> Pro<P, I, A, B>.wander(f: WanderF<S, T, A, B>): Pro<P, I, S, T>
  fun <I, J, S, T, A, B> Pro<P, J, A, B>.iwander(f: IxWanderF<I, S, T, A, B>): Pro<P, (I) -> J, S, T>
}

// Traverse in disguise
interface WanderF<S, T, A, B> {
  operator fun <F> invoke(AF: Applicative<F>, source: S, f: (A) -> Kind<F, B>): Kind<F, T>

  /**
   * Variant of invoke that generally uses `Applicative.apEval` or similar to lazily traverse
   *  a structure. Optics usually choose either invoke or invokeLazy on their terminal
   *  operation. E.g. firstOrNull uses invokeLazy to only traverse to the first element
   *  even if the traversal is infinite. Others such as sum will use invoke since it is
   *  strict in all elements anyway.
   */
  fun <F> invokeLazy(AF: Applicative<F>, source: S, f: (A) -> Kind<F, B>): Kind<F, T> = invoke(AF, source, f)
}

interface IxWanderF<I, S, T, A, B> {
  operator fun <F> invoke(AF: Applicative<F>, source: S, f: (I, A) -> Kind<F, B>): Kind<F, T>
  fun <F> invokeLazy(AF: Applicative<F>, source: S, f: (I, A) -> Kind<F, B>): Kind<F, T> = invoke(AF, source, f)
}

/**
 * Mapping is a stronger form of Traversing in which the applicative used is Id.
 * Rules out any effects, but also offers some flexibility. Used by setters.
 */
interface Mapping<P> : Traversing<P> {
  fun <I, S, T, A, B> Pro<P, I, A, B>.roam(f: (inner: (A) -> B, s: S) -> T): Pro<P, I, S, T>
  fun <I, J, S, T, A, B> Pro<P, J, A, B>.iroam(f: (inner: (I, A) -> B, s: S) -> T): Pro<P, (I) -> J, S, T>
}

// RETURN OF THE TYPECLASSES PART 2!
interface Functor<F> {
  fun <A, B> map(fa: Kind<F, A>, f: (A) -> B): Kind<F, B>
}

interface Applicative<F> : Functor<F> {
  fun <A> pure(a: A): Kind<F, A>
  fun <A, B> ap(ff: Kind<F, (A) -> B>, fa: Kind<F, A>): Kind<F, B>
  fun <A, B> apLazy(ff: Kind<F, (A) -> B>, fa: Eval<Kind<F, A>>): Eval<Kind<F, B>> =
    fa.map { fa -> ap(ff, fa) }

  override fun <A, B> map(fa: Kind<F, A>, f: (A) -> B): Kind<F, B> = ap(pure(f), fa)
}

// Helpers TODO Move
internal class ForId

internal fun <A> Kind<ForId, A>.fix(): Id<A> = this as Id<A>
internal class Id<A>(val v: A) : Kind<ForId, A>

internal interface IdApplicative : Applicative<ForId> {
  override fun <A> pure(a: A): Kind<ForId, A> = Id(a)
  override fun <A, B> ap(ff: Kind<ForId, (A) -> B>, fa: Kind<ForId, A>): Kind<ForId, B> =
    Id(ff.fix().v(fa.fix().v))

  override fun <A, B> map(fa: Kind<ForId, A>, f: (A) -> B): Kind<ForId, B> =
    Id(f(fa.fix().v))
}

internal class ForConst

internal fun <A, B> Kind<Kind<ForConst, A>, B>.fix(): Const<A, B> = this as Const<A, B>
internal class Const<A, B>(internal val v: A) : Kind<Kind<ForConst, A>, B>

internal interface ConstFunctor<R> : Functor<Kind<ForConst, R>> {
  override fun <A, B> map(fa: Kind<Kind<ForConst, R>, A>, f: (A) -> B): Kind<Kind<ForConst, R>, B> =
    fa as Const<R, B>
}

internal interface ConstApplicative<R> : Applicative<Kind<ForConst, R>>, ConstFunctor<R> {
  fun MR(): Monoid<R>
  override fun <A> pure(a: A): Kind<Kind<ForConst, R>, A> =
    Const(MR().empty())

  override fun <A, B> ap(
    ff: Kind<Kind<ForConst, R>, (A) -> B>,
    fa: Kind<Kind<ForConst, R>, A>
  ): Kind<Kind<ForConst, R>, B> =
    Const(MR().run { ff.fix().v.combine(fa.fix().v) })

  override fun <A, B> apLazy(
    ff: Kind<Kind<ForConst, R>, (A) -> B>,
    fa: Eval<Kind<Kind<ForConst, R>, A>>
  ): Eval<Kind<Kind<ForConst, R>, B>> =
    MR().run { ff.fix().v.combineLazy(fa.map { it.fix().v }).map { Const(it) } }

  override fun <A, B> map(fa: Kind<Kind<ForConst, R>, A>, f: (A) -> B): Kind<Kind<ForConst, R>, B> =
    fa as Const<R, B>
}
