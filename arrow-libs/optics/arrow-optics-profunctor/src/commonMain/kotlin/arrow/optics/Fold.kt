package arrow.optics

import arrow.core.identity
import arrow.optics.internal.Applicative
import arrow.optics.internal.Forget
import arrow.optics.internal.IxForget
import arrow.optics.internal.IxWanderF
import arrow.optics.internal.Kind
import arrow.optics.internal.Pro
import arrow.optics.internal.Profunctor
import arrow.optics.internal.Traversing
import arrow.optics.internal.WanderF
import arrow.optics.internal.fix
import arrow.typeclasses.Monoid

typealias Fold<S, A> = Optic<FoldK, Any?, S, Nothing, A, Nothing>

fun <S, A> Optic.Companion.folding(
  ff: FoldF<S, A>
): Fold<S, A> =
  object : Fold<S, A> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, Nothing>): Pro<P, (Any?) -> J, S, Nothing> =
      (this as Traversing<P>).run {
        // Casts are safe because P is guaranteed to be Forget here
        focus.wander(object : WanderF<S, Nothing, A, Nothing> {
          override fun <F> invoke(AF: Applicative<F>, source: S, f: (A) -> Kind<F, Nothing>): Kind<F, Nothing> =
            // These casts are somewhat safe
            ff.invoke(AF, source, f as (A) -> Kind<F, Unit>) as Kind<F, Nothing>

          override fun <F> invokeLazy(AF: Applicative<F>, source: S, f: (A) -> Kind<F, Nothing>): Kind<F, Nothing> =
            ff.invokeLazy(AF, source, f as (A) -> Kind<F, Unit>) as Kind<F, Nothing>
        })
      }.ixMap { it(Unit) }
  }

interface FoldF<S, A> {
  operator fun <F> invoke(AF: Applicative<F>, s: S, f: (A) -> Kind<F, Unit>): Kind<F, Unit>
  fun <F> invokeLazy(AF: Applicative<F>, s: S, f: (A) -> Kind<F, Unit>): Kind<F, Unit> = invoke(AF, s, f)
}

typealias IxFold<I, S, A> = Optic<FoldK, I, S, Nothing, A, Nothing>

fun <I, S, A> Optic.Companion.ixFolding(
  ff: IxFoldF<I, S, A>
): IxFold<I, S, A> =
  object : IxFold<I, S, A> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, Nothing>): Pro<P, (I) -> J, S, Nothing> =
      (this as Traversing<P>).run {
        // Casts are safe because P is guaranteed to be Forget here
        focus.iwander(object : IxWanderF<I, S, Nothing, A, Nothing> {
          override fun <F> invoke(AF: Applicative<F>, source: S, f: (I, A) -> Kind<F, Nothing>): Kind<F, Nothing> =
            ff.invoke(AF, source, f as (I, A) -> Kind<F, Unit>) as Kind<F, Nothing>

          override fun <F> invokeLazy(AF: Applicative<F>, source: S, f: (I, A) -> Kind<F, Nothing>): Kind<F, Nothing> =
            ff.invokeLazy(AF, source, f as (I, A) -> Kind<F, Unit>) as Kind<F, Nothing>
        })
      }.ixMap { f -> { i: I -> f(i) } }
  }

interface IxFoldF<I, S, A> {
  operator fun <F> invoke(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit>
  fun <F> invokeLazy(AF: Applicative<F>, s: S, f: (I, A) -> Kind<F, Unit>): Kind<F, Unit> = invoke(AF, s, f)
}

fun <K : FoldK, I, S, T, A, B, R> S.foldOf(optic: Optic<K, I, S, T, A, B>, MR: Monoid<R>, f: (A) -> R): R =
  Forget.traversing(MR).run { optic.run { transform(Forget<R, Nothing, A, B>(f)) } }
    .fix().f(this)

fun <K : FoldK, I, S, T, A, B, R> S.ixFoldOf(optic: Optic<K, I, S, T, A, B>, MR: Monoid<R>, f: (I, A) -> R): R =
  IxForget.traversing(MR).run { optic.run { transform(IxForget(f)) } }
    .fix().f(::identity, this)

fun <K : FoldK, I, S, T, A, B, R> S.foldLazyOf(optic: Optic<K, I, S, T, A, B>, MR: Monoid<R>, f: (A) -> R): R =
  Forget.traversingLazy(MR).run { optic.run { transform(Forget<R, Nothing, A, B>(f)) } }
    .fix().f(this)

fun <K : FoldK, I, S, T, A, B, R> S.ixFoldLazyOf(optic: Optic<K, I, S, T, A, B>, MR: Monoid<R>, f: (I, A) -> R): R =
  IxForget.traversingLazy(MR).run { optic.run { transform(IxForget(f)) } }
    .fix().f(::identity, this)
