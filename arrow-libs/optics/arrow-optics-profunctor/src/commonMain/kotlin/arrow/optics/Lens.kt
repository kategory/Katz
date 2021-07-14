package arrow.optics

import arrow.optics.internal.Functor
import arrow.optics.internal.IxLinearF
import arrow.optics.internal.Kind
import arrow.optics.internal.Pro
import arrow.optics.internal.Profunctor
import arrow.optics.internal.Strong

typealias Lens<S, A> = PLens<S, S, A, A>
typealias PLens<S, T, A, B> = Optic<LensK, Any?, S, T, A, B>

fun <S, T, A, B> Optic.Companion.lens(
  get: (S) -> A,
  set: (S, B) -> T
): PLens<S, T, A, B> =
  object : PLens<S, T, A, B> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, B>): Pro<P, (Any?) -> J, S, T> =
      (this as Strong<P>).run {
        focus.first<J, A, B, S>().dimap({ s: S ->
          get(s) to s
        }, { (b, s) ->
          set(s, b)
        }).ixMap { it(Unit) }
      }
  }

typealias IxLens<I, S, A> = PIxLens<I, S, S, A, A>
typealias PIxLens<I, S, T, A, B> = Optic<LensK, I, S, T, A, B>

fun <I, S, T, A, B> Optic.Companion.ixLens(
  get: (S) -> Pair<I, A>,
  set: (S, B) -> T
): PIxLens<I, S, T, A, B> =
  object : PIxLens<I, S, T, A, B> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, B>): Pro<P, (I) -> J, S, T> =
      (this as Strong<P>).run {
        focus.ilinear(object : IxLinearF<I, S, T, A, B> {
          override fun <F> invoke(FF: Functor<F>, s: S, f: (I, A) -> Kind<F, B>): Kind<F, T> =
            get(s).let { (i, a) -> FF.map(f(i, a)) { b -> set(s, b) } }
        })
      }
  }
