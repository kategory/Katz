package arrow.optics

import arrow.optics.internal.Forget
import arrow.optics.internal.Functor
import arrow.optics.internal.IxForget
import arrow.optics.internal.IxLinearF
import arrow.optics.internal.Kind
import arrow.optics.internal.Pro
import arrow.optics.internal.Profunctor
import arrow.optics.internal.Strong
import arrow.optics.internal.fix

typealias Getter<S, A> = Optic<GetterK, Any?, S, Nothing, A, Nothing>

fun <S, A> Optic.Companion.get(f: (S) -> A): Getter<S, A> =
  object : Getter<S, A> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, Nothing>): Pro<P, (Any?) -> J, S, Nothing> =
      // Safe cast because GetterK ensures we always use Forget for P
      focus.lMap(f).ixMap { it(Unit) }
  }

typealias IxGetter<I, S, A> = Optic<GetterK, I, S, Nothing, A, Nothing>

fun <S, I, A> Optic.Companion.ixGet(ff: (S) -> Pair<I, A>): IxGetter<I, S, A> =
  object : IxGetter<I, S, A> {
    override fun <P, J> Profunctor<P>.transform(focus: Pro<P, J, A, Nothing>): Pro<P, (I) -> J, S, Nothing> =
      (this as Strong<P>).run {
        focus.ilinear(object : IxLinearF<I, S, Nothing, A, Nothing> {
          override fun <F> invoke(FF: Functor<F>, s: S, f: (I, A) -> Kind<F, Nothing>): Kind<F, Nothing> =
            ff(s).let { (i, a) -> FF.map(f(i, a)) { s } as Kind<F, Nothing> }
        })
      }
  }

fun <K : GetterK, I, S, T, A, B> S.view(optic: Optic<K, I, S, T, A, B>): A =
  Forget.strong<A>().run { optic.run { transform(Forget<A, Nothing, A, B> { it }) } }
    .fix().f(this)

fun <K : GetterK, I, S, T, A, B> S.ixView(optic: Optic<K, I, S, T, A, B>): Pair<I, A> =
  IxForget.strong<Pair<I, A>>().run { optic.run { transform(IxForget { i: I, a: A -> i to a }) } }
    .fix().f({ it }, this)
