package arrow.optics

import arrow.core.Either
import arrow.core.identity
import arrow.optics.internal.Applicative
import arrow.optics.internal.Choice
import arrow.optics.internal.IxWanderF
import arrow.optics.internal.Kind
import arrow.optics.internal.Pro
import arrow.optics.internal.Profunctor
import arrow.optics.internal.Strong
import arrow.optics.internal.Traversing

typealias AffineTraversal<S, A> = PAffineTraversal<S, S, A, A>
typealias PAffineTraversal<S, T, A, B> = Optic<AffineTraversalK, Any?, S, T, A, B>

fun <S, T, A, B> Optic.Companion.aTraversing(
  match: (S) -> Either<T, A>,
  update: (S, B) -> T
): PAffineTraversal<S, T, A, B> =
  object : PAffineTraversal<S, T, A, B> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, B>): Pro<P, (Any?) -> J, S, T> {
      val r = (this as Choice<P>).run {
        focus.right<J, A, B, T>()
      }
      return (this as Strong<P>).run {
        r.first<J, Either<T, A>, Either<T, B>, S>().dimap({ s: S ->
          match(s) to s
        }, { (e, s) ->
          e.fold(::identity) { b: B -> update(s, b) }
        }).ixMap { it(Unit) }
      }
    }
  }

typealias IxAffineTraversal<I, S, A> = PIxAffineTraversal<I, S, S, A, A>
typealias PIxAffineTraversal<I, S, T, A, B> = Optic<AffineTraversalK, I, S, T, A, B>

fun <I, S, T, A, B> Optic.Companion.ixATraversing(
  match: (S) -> Either<T, Pair<I, A>>,
  update: (S, B) -> T
): PIxAffineTraversal<I, S, T, A, B> =
  object : PIxAffineTraversal<I, S, T, A, B> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, B>): Pro<P, (I) -> J, S, T> =
      (this as Traversing<P>).run {
        focus.iwander(object : IxWanderF<I, S, T, A, B> {
          override fun <F> invoke(AF: Applicative<F>, source: S, f: (I, A) -> Kind<F, B>): Kind<F, T> =
            match(source).fold({ t ->
              AF.pure(t)
            }, { (i, a) ->
              AF.map(f(i, a)) { b -> update(source, b) }
            })
        })
      }
  }
