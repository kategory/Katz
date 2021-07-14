package arrow.optics

import arrow.core.Either
import arrow.core.identity
import arrow.optics.internal.Choice
import arrow.optics.internal.Forget
import arrow.optics.internal.IxForget
import arrow.optics.internal.Pro
import arrow.optics.internal.Profunctor
import arrow.optics.internal.fix
import arrow.typeclasses.Monoid

typealias AffineFold<S, A> = Optic<AffineFoldK, Any?, S, Nothing, A, Nothing>

fun <S, A> Optic.Companion.aFolding(
  f: (S) -> A?
): AffineFold<S, A> =
  object : AffineFold<S, A> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, Nothing>): Pro<P, (Any?) -> J, S, Nothing> =
      (this as Choice<P>).run {
        focus.left<J, A, Nothing, Any?>().lMap { s: S ->
          f(s)?.let { Either.Left(it) } ?: Either.Right(Unit)
        } as Pro<P, J, S, Nothing> // Safe because AffineFoldK guarantees use of Forget
      }.ixMap { it(Unit) }
  }

typealias IxAffineFold<I, S, A> = Optic<AffineFoldK, I, S, Nothing, A, Nothing>

fun <I, S, A> Optic.Companion.ixAFolding(
  f: (S) -> Pair<I, A>?
): IxAffineFold<I, S, A> =
  object : IxAffineFold<I, S, A> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, Nothing>): Pro<P, (I) -> J, S, Nothing> =
      (this as Choice<P>).run {
        focus.left<J, A, Nothing, Any?>().lMap { s: S ->
          f(s)?.let { Either.Left(it.second) } ?: Either.Right(Unit)
        } as Pro<P, (I) -> J, S, Nothing> // Safe because AffineFoldK guarantees use of Forget
      }.ixMap { f -> { i: I -> f(i) } }
  }

fun <K : AffineFoldK, I, S, T, A, B> S.viewOrNull(optic: Optic<K, I, S, T, A, B>): A? =
  Forget.traversing(Monoid.first<A>()).run {
    optic.run {
      transform(Forget<A?, I, A, B> { it })
    }
  }.fix().f(this)

fun <K : AffineFoldK, I, S, T, A, B> S.ixViewOrNull(optic: Optic<K, I, S, T, A, B>): Pair<I, A>? =
  IxForget.traversing(Monoid.first<Pair<I, A>>()).run {
    optic.run {
      transform(IxForget<Pair<I, A>?, I, A, B> { i, a -> i to a })
    }
  }.fix().f(::identity, this)

fun <K : AffineFoldK, I, S, T, A, B> S.preview(optic: Optic<K, I, S, T, A, B>): A? =
  viewOrNull(optic)

fun <K : AffineFoldK, I, S, T, A, B> S.ixPreview(optic: Optic<K, I, S, T, A, B>): Pair<I, A>? =
  ixViewOrNull(optic)
