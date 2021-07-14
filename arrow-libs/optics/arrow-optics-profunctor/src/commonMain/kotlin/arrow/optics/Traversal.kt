package arrow.optics

import arrow.core.identity
import arrow.optics.internal.Applicative
import arrow.optics.internal.IxStar
import arrow.optics.internal.IxWanderF
import arrow.optics.internal.Kind
import arrow.optics.internal.Pro
import arrow.optics.internal.Profunctor
import arrow.optics.internal.Star
import arrow.optics.internal.Traversing
import arrow.optics.internal.WanderF
import arrow.optics.internal.fix

typealias Traversal<S, A> = PTraversal<S, S, A, A>
typealias PTraversal<S, T, A, B> = Optic<TraversalK, Any?, S, T, A, B>

fun <S, T, A, B> Optic.Companion.traversing(
  f: WanderF<S, T, A, B>
): PTraversal<S, T, A, B> =
  object : PTraversal<S, T, A, B> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, B>): Pro<P, (Any?) -> J, S, T> =
      (this as Traversing<P>).run {
        focus.wander(f).ixMap { it(Unit) }
      }
  }

typealias IxTraversal<I, S, A> = PIxTraversal<I, S, S, A, A>
typealias PIxTraversal<I, S, T, A, B> = Optic<TraversalK, I, S, T, A, B>

fun <I, S, T, A, B> Optic.Companion.ixTraversing(
  f: IxWanderF<I, S, T, A, B>
): PIxTraversal<I, S, T, A, B> =
  object : PIxTraversal<I, S, T, A, B> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, B>): Pro<P, (I) -> J, S, T> =
      (this as Traversing<P>).run {
        focus.iwander(f)
      }
  }

fun <K : TraversalK, I, S, T, A, B, F> S.traverseOf(
  optic: Optic<K, I, S, T, A, B>,
  AF: Applicative<F>,
  f: (A) -> Kind<F, B>
): Kind<F, T> =
  Star.traversing(AF).run { optic.run { transform(Star<F, I, A, B>(f)) } }
    .fix().f(this)

fun <K : TraversalK, I, S, T, A, B, F> S.ixTraverseOf(
  optic: Optic<K, I, S, T, A, B>,
  AF: Applicative<F>,
  f: (I, A) -> Kind<F, B>
): Kind<F, T> =
  IxStar.traversing(AF).run { optic.run { transform(IxStar(f)) } }
    .fix().f(::identity, this)

fun <K : TraversalK, I, S, T, A, B, F> S.traverseLazyOf(
  optic: Optic<K, I, S, T, A, B>,
  AF: Applicative<F>,
  f: (A) -> Kind<F, B>
): Kind<F, T> =
  Star.traversingLazy(AF).run { optic.run { transform(Star<F, I, A, B>(f)) } }
    .fix().f(this)

fun <K : TraversalK, I, S, T, A, B, F> S.ixTraverseLazyOf(
  optic: Optic<K, I, S, T, A, B>,
  AF: Applicative<F>,
  f: (I, A) -> Kind<F, B>
): Kind<F, T> =
  IxStar.traversingLazy(AF).run { optic.run { transform(IxStar(f)) } }
    .fix().f(::identity, this)

fun <K : TraversalK, I, S, T, A, B> S.modifyOrNull(optic: Optic<K, I, S, T, A, B>, f: (A) -> B): T? {
  var tripped = false
  val res = this.modify(optic) { a -> f(a).also { tripped = true } }
  return if (tripped) res else null
}

fun <K : TraversalK, I, S, T, A, B> S.ixModifyOrNull(optic: Optic<K, I, S, T, A, B>, f: (I, A) -> B): T? {
  var tripped = false
  val res = this.ixModify(optic) { i, a -> f(i, a).also { tripped = true } }
  return if (tripped) res else null
}
